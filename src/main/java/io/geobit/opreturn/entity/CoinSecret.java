package io.geobit.opreturn.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfNotNull;

import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Riccardo Casatta @RCasatta on 30/05/15.
 */
@Entity
public class CoinSecret implements Serializable {
    @Id
    private Long id;

    private String json;


    @Index({IfNotNull.class})
    private String day;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    private final static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    public static CoinSecret fromJSON(String s) {
        try {
            CoinSecret coinSecret=new CoinSecret();
            JSONObject obj=new JSONObject(s);
            String height = obj.getString("height");
            if(obj.has("timestamp") && !obj.isNull("timestamp")) {
                String timestamp = obj.getString("timestamp");
                coinSecret.setId(Long.parseLong(height));
                coinSecret.setDay(dayFormat.format(new Date(Long.parseLong(timestamp) * 1000)));
            }
            coinSecret.setJson(s);

            return coinSecret;
        } catch (Exception e) {

        }
        return null;
    }
}
