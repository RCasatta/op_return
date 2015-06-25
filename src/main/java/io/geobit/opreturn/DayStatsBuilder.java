package io.geobit.opreturn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecret;
import io.geobit.opreturn.entity.CoinSecretGrouped;


/**
 * Created by Riccardo Casatta @RCasatta on 18/04/15.
 */
public class DayStatsBuilder extends HttpServlet {
    private final static Logger log  = Logger.getLogger(DayStatsBuilder.class.getName());
    private final static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    protected void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String cronHeader = req.getHeader("X-AppEngine-Cron");
        final String debug      = req.getParameter("debug");

        if( "true".equals(cronHeader) || "true".equals(debug) ) {
            String    day = req.getParameter("day");
            if(day==null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.HOUR, -24);
                day = dayFormat.format(cal.getTime());
            }
            log.info("day=" + day);

            final Map<String, Integer> counters = new HashMap<>();
            final Map<String, Integer> hexCounters = new HashMap<>();
            List<CoinSecret> coinSecretList = OfyService.ofy().load().type(CoinSecret.class).filter("day",day).list();
            if(coinSecretList.size()>0) {
                final CoinSecretGrouped coinSecretGrouped = new CoinSecretGrouped();

                final Integer totalBlocksWithOpReturns = coinSecretList.size();
                coinSecretGrouped.setBlocksWithOpReturn(totalBlocksWithOpReturns);
                coinSecretGrouped.setDay(day);
                log.info("totalBlocksWithOpReturns " + totalBlocksWithOpReturns);
                Integer totalTxWithOpReturns = 0;

                for (CoinSecret coinSecret : coinSecretList) {
                    final String json = coinSecret.getJson();
                    try {
                        JSONObject obj = new JSONObject(json);
                        JSONArray arr = obj.getJSONArray("op_returns");
                        totalTxWithOpReturns +=arr.length();
                        for (int i = 0; i < arr.length(); i++) {
                            final JSONObject jsonObject = arr.getJSONObject(i);
                            if (jsonObject.has("ascii")) {
                                String ascii = jsonObject.getString("ascii");
                                if (ascii.length() >= 3) {
                                    final String key = ascii.substring(0, 3);
                                    final int count = counters.containsKey(key) ? counters.get(key) : 0;
                                    counters.put(key, count + 1);
                                }
                            }
                            if (jsonObject.has("hex")) {
                                String ascii = jsonObject.getString("hex");
                                if (ascii.length() >= 6) {
                                    final String key = ascii.substring(0, 6);
                                    final int count = hexCounters.containsKey(key) ? hexCounters.get(key) : 0;
                                    hexCounters.put(key, count + 1);

                                }
                            }

                        }
                    } catch (Exception e) {
                    }
                }
                coinSecretGrouped.setTxWithOpReturn(totalTxWithOpReturns);
                JSONObject obj = new JSONObject(hexCounters);
                coinSecretGrouped.setJson(obj.toString());

                OfyService.ofy().save().entity(coinSecretGrouped).now();
            }
            resp.setContentType("text/plain");
            resp.getWriter().write("" + counters.toString() + "\n" + hexCounters.toString() );
        } else {
            log.warning("no cron or debug parameter to true");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }
}
