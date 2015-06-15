package io.geobit.opreturn;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Riccardo Casatta @RCasatta on 18/04/15.
 */
public class DayStatsBuilderCaller extends HttpServlet {
    private final static Logger log  = Logger.getLogger(DayStatsBuilderCaller.class.getName());
    private final static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    private Queue queue = QueueFactory.getDefaultQueue();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String cronHeader = req.getHeader("X-AppEngine-Cron");
        final String debug      = req.getParameter("debug");
        final String from       = req.getParameter("from");
        final String to         = req.getParameter("to");

        if( "true".equals(cronHeader) || "true".equals(debug) ) {
            try {
                Date fromDate = dayFormat.parse(from);
                Date toDate   = dayFormat.parse(to);
                Date now = new Date();
                if(fromDate.before(toDate)) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(fromDate);
                    StringBuffer stringBuffer=new StringBuffer();
                    int max=90;
                    do {
                        String currentDateString = dayFormat.format(cal.getTime());
                        stringBuffer.append(currentDateString);
                        stringBuffer.append("\n");

                        final String url = "/v1/coinsecret/build";
                        log.info("calling url " + url + " with day " + currentDateString);

                        queue.add(TaskOptions.Builder.withUrl(url)
                                .param("day", currentDateString)
                                .param("debug", "true")
                                .retryOptions(RetryOptions.Builder.withTaskRetryLimit(1)));

                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        max--;
                        log.info("cal.getTime()=" + cal.getTime() + " toDate=" + toDate + " now=" + now);

                    } while(cal.getTime().before(toDate) && max>0 && now.after(toDate) );
                    resp.setContentType("text/plain");
                    resp.getWriter().write(stringBuffer.toString());

                }
            } catch (ParseException e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
