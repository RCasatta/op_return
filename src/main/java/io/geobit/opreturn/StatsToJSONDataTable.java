package io.geobit.opreturn;


import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecretGrouped;

/**
 * Created by Riccardo Casatta @RCasatta on 19/04/15.
 */
public class StatsToJSONDataTable extends HttpServlet {
    private final static Logger log  = Logger.getLogger(StatsToJSONDataTable.class.getName());
    private final static Map<String,String> map= new HashMap<>();
    private final static Map<String,String> hexGrouping= new HashMap<>();
    private MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private final static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    private final static SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");
    private final static Map<String,String> colorMap= new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        initMaps();

    }

    private void initMaps() {
        map.put("BIT", "Bitproof");
        map.put("DOC", "Docproof");
        map.put("FAC", "Factom");  //464143
        map.put("SPK", "Coinspark");
        map.put(new String(fromHex("4f4101")) , "Open Assets");
        map.put(new String(fromHex("434301")) , "Colu");
        map.put("ASC", "Ascribe pool");  //415343
        map.put("EW ", "Eternity Wall");  //455720
        map.put(new String(fromHex("4d4700")), "Monegraph");
        map.put("id;", "Blockstack");  //69643b
        map.put("CNT", "Counterparty");
        map.put("omn", "Omni Layer");
        map.put(new String(fromHex("533400")) , "Stampery Old"); //this is not stampery
        map.put(new String(fromHex("533500")) , "Stampery 5"); //this is not stampery


        hexGrouping.put("4d47ff", "4d4700");  // MG? -> MG?
        hexGrouping.put("455741", "455720");  // EWA -> EW
        hexGrouping.put("455743", "455720");  // EWC -> EW
        hexGrouping.put("466100", "464143");  // Fa? -> FAC
        hexGrouping.put("69642b", "69643b");  // id+ -> id;
        hexGrouping.put("69643a", "69643b");  // id: -> id;
        hexGrouping.put("69643f", "69643b");  // id? -> id;
        hexGrouping.put("69643e", "69643b");  // id> -> id;
        hexGrouping.put("434302", "434301");  // CC2 -> CC1

        putAllCombination(hexGrouping, "5334", "533400");
        putAllCombination(hexGrouping, "5335", "533500");


        colorMap.put("455720", "#0089F3");  //Eternity Wall
        colorMap.put("69643b", "#53206f");  //Blockstack
        colorMap.put("434301", "#44DAB4");  //Colu

        /*
        colorMap.put("4d4700", "#a82991");  //Monegraph
        colorMap.put("464143", "#fc6b22");  //Factom
        colorMap.put("415343", "#67C4DA");  //Ascribe
        colorMap.put("4f4101", "#DC3912");  //Open Assets
        */

        //0x4f 0x43 <transaction count (8 bytes)> <cumulative hash (32 bytes)>

    }

    private void putAllCombination(Map<String, String> hexGrouping, String key, String mapTo) {
        for(int i=0;i<16*16;i++) {
            final String x = Integer.toHexString(i);
            final String y = x.length()<2 ? "0" + x : x;
            final String value = key + y;
            if(!key.equals(value)) {
                hexGrouping.put(value, mapTo);
            }
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {

            final Calendar cal = Calendar.getInstance();
            final String currentMonth = monthFormat.format(cal.getTime());
            log.info("currentMonth:" + currentMonth);
            cal.add(Calendar.HOUR, -24);
            final String dayKey = "statsOf" + dayFormat.format(cal.getTime());

            String result;
            final Object cached = memcache.get(dayKey);

            if(cached==null) {
                List<CoinSecretGrouped> coinSecretGroupedList = OfyService.ofy().load().type(CoinSecretGrouped.class).list();
                JSONObject dataTable = new JSONObject();

                JSONObject total = new JSONObject();
                JSONArray totalCols = new JSONArray();
                JSONArray totalRows = new JSONArray();
                JSONObject totalCol0 = new JSONObject();
                JSONObject totalCol1 = new JSONObject();
                JSONArray cumulativeColors = new JSONArray();
                totalCol0.put("id", "x");
                totalCol1.put("id", "A");
                totalCol0.put("label", "date");
                totalCol1.put("label", "total");
                totalCol0.put("type", "date");
                totalCol1.put("type", "number");
                totalCols.put(totalCol0);
                totalCols.put(totalCol1);
                total.put("cols", totalCols);

                final Map<String, Integer> counters = new HashMap<>();
                final Map<String, Integer> lastWeekCounters = new HashMap<>();

                final Map<String, Map<String,Integer>> monthGrouped = new HashMap<>();
                final Set<String> months=new HashSet<>();

                int size=coinSecretGroupedList.size();
                int lastWeek=size-7;
                log.info("total CoinSecretGrouped " + size);
                int cont=0;

                for (CoinSecretGrouped coinSecretGrouped : coinSecretGroupedList) {
                    String data = coinSecretGrouped.getDay();
                    String year = data.substring(0, 4);
                    String month = data.substring(4, 6);
                    String day = data.substring(6, 8);
                    String jDate = String.format("Date(%s,%d,%s)", year, Integer.parseInt(month) - 1, day);

                    JSONObject totalC = new JSONObject();
                    JSONArray totalCArr = new JSONArray();
                    JSONObject totalV0 = new JSONObject();
                    JSONObject totalV1 = new JSONObject();
                    totalV0.put("v", jDate);
                    totalV1.put("v", coinSecretGrouped.getTxWithOpReturn());
                    totalCArr.put(totalV0);
                    totalCArr.put(totalV1);
                    totalC.put("c", totalCArr);
                    totalRows.put(totalC);

                    final String hexJSON = coinSecretGrouped.getJson();
                    if (hexJSON != null) {
                        JSONObject jsonObject = new JSONObject(hexJSON);
                        Iterator<?> keys = jsonObject.keys();
                        while (keys.hasNext()) {
                            final String hex = (String) keys.next();
                            final String groupedHex = hexGrouping.get(hex)!=null ? hexGrouping.get(hex) : hex;
                            //log.info(hex + " " + hexInString + " " + key );

                            int count = counters.containsKey(groupedHex) ? counters.get(groupedHex) : 0;
                            final int anInt = jsonObject.getInt(hex);
                            counters.put(groupedHex, count + anInt);
                            if(cont>lastWeek) {
                                int countW = lastWeekCounters.containsKey(groupedHex) ? lastWeekCounters.get(groupedHex) : 0;
                                lastWeekCounters.put(groupedHex, countW + anInt);
                            }

                            final String key = year + month;
                            months.add(key);
                            Map<String, Integer> stringIntegerMap = monthGrouped.get(key);
                            if(stringIntegerMap==null) {
                                stringIntegerMap=new HashMap<>();
                                monthGrouped.put(key,stringIntegerMap);
                            }
                            final Integer integer = stringIntegerMap.get(groupedHex);
                            if(integer==null) {
                                stringIntegerMap.put(groupedHex,anInt);
                            } else {
                                stringIntegerMap.put(groupedHex,integer+anInt);
                            }
                        }

                    }
                    cont++;
                }
                total.put("rows", totalRows);

                final Map<String, Integer> moreThanXCounters = new HashMap<>();
                final Map<String, Integer> moreThanYCounters = new HashMap<>();

                for (Map.Entry<String, Integer> entry : counters.entrySet()) {
                    if (entry.getValue() > 3000)
                        moreThanXCounters.put(entry.getKey(), entry.getValue());
                    if (entry.getValue() > 3000)
                        moreThanYCounters.put(entry.getKey(), entry.getValue());
                }

                final Map<String, Integer> moreThanXLastWeekCounters = new HashMap<>();

                for (Map.Entry<String, Integer> entry : lastWeekCounters.entrySet()) {
                    if (entry.getValue() > 100)
                        moreThanXLastWeekCounters.put(entry.getKey(), entry.getValue());
                }

                log.info("counters.size()=" + counters.size() + "\nmoreThanXCounters.size()=" + moreThanXCounters.size() +
                        "\nmoreThanYCounters.size()=" + moreThanYCounters.size() + "\nlastWeekCounters.size()=" + lastWeekCounters.size() +
                        "\nmoreThanXLastWeekCounters.size()=" + moreThanXLastWeekCounters.size());


                JSONObject byProto = new JSONObject();
                JSONArray byProtoCols = new JSONArray();
                JSONArray byProtoRows = new JSONArray();
                JSONArray byProtoColors = new JSONArray();
                JSONObject byProtoCol0 = new JSONObject();
                byProtoCol0.put("id", "x");
                byProtoCol0.put("label", "date");
                byProtoCol0.put("type", "date");
                byProtoCols.put(byProtoCol0);
                List<String> moreThanXCountersHex=new LinkedList<>();
                for (Map.Entry<String, Integer> entry : moreThanXCounters.entrySet()) {
                    JSONObject byProtoColX = new JSONObject();
                    final String o = entry.getKey();
                    moreThanXCountersHex.add(o);
                    byProtoColX.put("id", o);
                    byProtoColX.put("label", hexKeyToDesc(o));
                    byProtoColX.put("type", "number");
                    byProtoCols.put(byProtoColX);
                }
            /*
            JSONObject byProtoColLast = new JSONObject();
            byProtoColLast.put("id", "other");
            byProtoColLast.put("label",  "OTHER" );
            byProtoColLast.put("type", "number");
            byProtoCols.put(byProtoColLast);
            */
                byProto.put("cols", byProtoCols);
                List<String> monthsList=new LinkedList<>(months);
                Collections.sort(monthsList);
                for (String yearmonth : monthsList) {
                    String year = yearmonth.substring(0, 4);
                    String month = yearmonth.substring(4, 6);
                    if(currentMonth.equals(year + month)) {
                        log.info("currentMonth, skipping " + currentMonth);
                    } else {
                        String jDate = String.format("Date(%s,%d,1)", year, Integer.parseInt(month) - 1);
                        JSONObject currC = new JSONObject();
                        JSONArray currCArr = new JSONArray();
                        currC.put("c", currCArr);
                        JSONObject currV0 = new JSONObject();
                        currV0.put("v", jDate);
                        currCArr.put(currV0);
                        final Map<String, Integer> stringIntegerMap = monthGrouped.get(yearmonth);
                        for (String s : moreThanXCountersHex) {
                            final Integer integer = stringIntegerMap.get(s);
                            JSONObject currV1 = new JSONObject();
                            currV1.put("v", integer);
                            currCArr.put(currV1);
                        }
                        byProtoRows.put(currC);
                    }

                }
                for (String s : moreThanXCountersHex) {
                    byProtoColors.put(colorOf(s));
                }
                 /*
                byProto.put("cols", byProtoCols);
                for (CoinSecretGrouped coinSecretGrouped : coinSecretGroupedList) {
                    String data = coinSecretGrouped.getDay();
                    String year = data.substring(0, 4);
                    String month = data.substring(4, 6);
                    String day = data.substring(6, 8);
                    String jDate = String.format("Date(%s,%d,%s)", year, Integer.parseInt(month) - 1, day);
                    JSONObject currC = new JSONObject();
                    JSONArray currCArr = new JSONArray();
                    currC.put("c", currCArr);
                    JSONObject currV0 = new JSONObject();
                    currV0.put("v", jDate);
                    currCArr.put(currV0);

                    final String hexJSON = coinSecretGrouped.getJson();
                    if (hexJSON != null) {
                        JSONObject jsonObject = new JSONObject(hexJSON);
                        for (Map.Entry<String, Integer> entry : moreThanXCounters.entrySet()) {
                            final String key = entry.getKey();
                            final int val = jsonObject.has(key) ? jsonObject.getInt(key) : 0;
                            JSONObject currV1 = new JSONObject();
                            currV1.put("v", val);
                            currCArr.put(currV1);
                        }
                    }
                    byProtoRows.put(currC);
                }*/

                byProto.put("rows", byProtoRows);

                JSONObject cumulative = new JSONObject();
                JSONArray cumulativeCols = new JSONArray();
                JSONArray cumulativeRows = new JSONArray();
                JSONObject cumulativeCol0 = new JSONObject();
                JSONObject cumulativeCol1 = new JSONObject();
                cumulativeCol0.put("id", "x");
                cumulativeCol1.put("id", "A");
                cumulativeCol0.put("label", "names");
                cumulativeCol1.put("label", "values");
                cumulativeCol0.put("type", "string");
                cumulativeCol1.put("type", "number");
                cumulativeCols.put(cumulativeCol0);
                cumulativeCols.put(cumulativeCol1);
                cumulative.put("cols", cumulativeCols);
                for (Map.Entry<String, Integer> entry : moreThanYCounters.entrySet()) {
                    JSONObject cumulativeC = new JSONObject();
                    JSONArray cumulativeCArr = new JSONArray();
                    JSONObject cumulativeV0 = new JSONObject();
                    JSONObject cumulativeV1 = new JSONObject();


                    final String key = entry.getKey();
                    log.info(key + " maps to " + hexKeyToDesc(key));
                    cumulativeV0.put("v", hexKeyToDesc(key));
                    cumulativeV1.put("v", entry.getValue());
                    cumulativeCArr.put(cumulativeV0);
                    cumulativeCArr.put(cumulativeV1);
                    cumulativeC.put("c", cumulativeCArr);
                    cumulativeRows.put(cumulativeC);
                    cumulativeColors.put(colorOf(key));
                }
                cumulative.put("rows", cumulativeRows);


                JSONObject week = new JSONObject();
                JSONArray weekCols = new JSONArray();
                JSONArray weekRows = new JSONArray();
                JSONArray weekColors = new JSONArray();
                JSONObject weekCol0 = new JSONObject();
                JSONObject weekCol1 = new JSONObject();
                weekCol0.put("id", "x");
                weekCol1.put("id", "A");
                weekCol0.put("label", "names");
                weekCol1.put("label", "values");
                weekCol0.put("type", "string");
                weekCol1.put("type", "number");
                weekCols.put(weekCol0);
                weekCols.put(weekCol1);
                week.put("cols", weekCols);
                for (Map.Entry<String, Integer> entry : moreThanXLastWeekCounters.entrySet()) {
                    JSONObject weekC = new JSONObject();
                    JSONArray weekCArr = new JSONArray();
                    JSONObject weekV0 = new JSONObject();
                    JSONObject weekV1 = new JSONObject();

                    final String key = entry.getKey();
                    weekV0.put("v", hexKeyToDesc(key));
                    weekV1.put("v", entry.getValue());
                    weekCArr.put(weekV0);
                    weekCArr.put(weekV1);
                    weekC.put("c", weekCArr);
                    weekRows.put(weekC);
                    weekColors.put(colorOf(key));
                }
                week.put("rows", weekRows);


                dataTable.put("total", total);
                dataTable.put("cumulative", cumulative);
                dataTable.put("cumulativeColors", cumulativeColors);
                dataTable.put("proto", byProto);
                dataTable.put("protoColors", byProtoColors);
                //dataTable.put("counters", new JSONObject(moreThanXCounters));
                dataTable.put("week", week);
                dataTable.put("weekColors", weekColors);



                result = dataTable.toString();
                memcache.put(dayKey, result);

            } else {
                result = cached.toString();
            }
            GCSService.writeJson("op_return_stats.json",result);
            resp.setContentType("application/json");
            resp.getWriter().write(result);

        } catch (JSONException e) {
            log.warning(e + " " + e.getMessage());
        }
    }

    //Google chart colors http://there4.io/2012/05/02/google-chart-color-list/
    private static String defaultColors[]= {"#3366CC","#FF9900","#109618","#990099","#3B3EAC","#0099C6","#DD4477","#66AA00","#B82E2E","#316395","#994499","#22AA99","#AAAA11","#6633CC","#E67300","#8B0707","#329262","#5574A6","#3B3EAC"};
    private static int currDefaultColor=0;
    private String colorOf(String s) {
        final String s1 = colorMap.get(s);
        if(s1==null) {
            if(currDefaultColor<defaultColors.length-1) {
                final String defaultColor = defaultColors[currDefaultColor];
                colorMap.put(s, defaultColor);
                currDefaultColor++;
                return defaultColor;
            }

            return "#" + s;
        }
        else
            return s1;
    }

    private boolean isPrintable(String s) {
        System.out.println();
        for(int i=0;i<s.length();i+=2) {
            char c = s.charAt(i);
            if(c=='0' || c=='1' || c=='8' || c=='9' || c=='a' || c=='b' || c=='c' || c=='d' || c=='e' || c=='f' )
                return false;
        }
        return true;
    }


    private String hexKeyToDesc(String key) {
        final String val = new String(fromHex(key));
        if(map.containsKey(val))
            return map.get(val);
        else {
            if(isPrintable(key))
                return val;
            else {
                return "0x" + key;
            }
        }

    }




    public static byte[] fromHex(String s) {
        if (s != null) {
            try {
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    if (!Character.isWhitespace(ch)) {
                        sb.append(ch);
                    }
                }
                s = sb.toString();
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    int hi = (Character.digit(s.charAt(i), 16) << 4);
                    int low = Character.digit(s.charAt(i + 1), 16);
                    if (hi >= 256 || low < 0 || low >= 16) {
                        return null;
                    }
                    data[i / 2] = (byte) (hi | low);
                }
                return data;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
