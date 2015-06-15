package io.geobit.opreturn;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Optional;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecret;

/**
 * Created by Riccardo Casatta @RCasatta on 18/04/15.
 */
public class CoinSecretIndexCaller extends HttpServlet {
    private final static Logger log  = Logger.getLogger(CoinSecretIndexCaller.class.getName());
    private final static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    private Queue queue = QueueFactory.getDefaultQueue();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String cronHeader = req.getHeader("X-AppEngine-Cron");
        final String debug      = req.getParameter("debug");

        if( "true".equals(cronHeader) || "true".equals(debug) ) {
            try {
                log.info("entering");

                List<CoinSecret> loginAccessList = OfyService.ofy().load().type(CoinSecret.class).orderKey(true).limit(1).list();
                resp.setContentType("text/plain");
                log.info("loginAccessList.size()=" + loginAccessList.size());
                Long id;
                if(loginAccessList.size()>0)
                    id = loginAccessList.get(0).getId();
                else
                    id = 336861L; //first block of 2015
                Optional<String> optResponse = Http.get("http://btc.blockr.io/api/v1/block/info/last");
                if(optResponse.isPresent()) {
                    JSONObject obj=new JSONObject(optResponse.get());
                    Long last = obj.getJSONObject("data").getLong("nb");
                    log.info("last block=" + last);

                    final Long todo = last - id;
                    if(todo>0) {
                        log.warning("asking " + todo + " blocks");
                        for (int i = 0; i < todo; i++) {
                            queue.add(TaskOptions.Builder.withUrl( "/v1/coinsecret/index" )
                                    .param("height", String.valueOf(id + i + 1) )
                                    .countdownMillis(1000 * i )
                                    .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1)) );
                        }
                    } else {
                        log.warning("todo blocks negative " + todo);
                    }
                }
            } catch (Exception e) {
                log.warning("exception " + e );

                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            }




        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }
}
