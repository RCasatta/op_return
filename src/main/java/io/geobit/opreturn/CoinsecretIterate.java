package io.geobit.opreturn;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecret;

/**
 * Created by Riccardo Casatta @RCasatta on 16/06/15.
 */
public class CoinsecretIterate extends HttpServlet {
    private final static Logger log  = Logger.getLogger(CoinSecretIndex.class.getName());


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Iterable<CoinSecret> cars = OfyService.ofy().load().type(CoinSecret.class).iterable();
        StringBuffer sb = new StringBuffer();
        int total=0;
        Set<String> unique=new HashSet<>();

        for (CoinSecret car : cars) {
            String json=car.getJson();
            total++;
            try {
                JSONObject object = new JSONObject(json);
                JSONArray arr = object.getJSONArray("op_returns");
                for (int i = 0; i < arr.length(); i++) {
                    final JSONObject jsonObject = arr.getJSONObject(i);
                    if (jsonObject.has("ascii")) {
                        String ascii = jsonObject.getString("ascii");
                        String hex = jsonObject.getString("hex");
                        String txid = jsonObject.getString("txid");


                        if(!ascii.contains("ASCRIBES") && !ascii.contains("https://cpr.sm") && !ascii.contains("FACTOM") && !hex.startsWith("4f4101")
                               && !ascii.startsWith("DOCPROOF") && !ascii.startsWith("BITPROOF") && !unique.contains(ascii)) {
                            unique.add(ascii);
                            int count = 0;
                            final int length = ascii.length();
                            for (int j = 0; j < length; j++) {
                                if (Character.isLetterOrDigit(ascii.charAt(j)))
                                    count++;
                            }

                            if (count > length / 2) {
                                sb.append(txid);
                                sb.append(" ");
                                sb.append(ascii);
                                sb.append('\n');

                            }
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        log.info("total=" + total);
        resp.setContentType("text/plain");
        resp.getWriter().write(sb.toString());

    }
}
