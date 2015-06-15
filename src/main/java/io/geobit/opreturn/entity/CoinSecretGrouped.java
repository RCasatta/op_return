package io.geobit.opreturn.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.io.Serializable;

/**
 * Created by Riccardo Casatta @RCasatta on 30/05/15.
 */

@Entity
public class CoinSecretGrouped implements Serializable {

    @Id
    private String day;

    private Integer blocksWithOpReturn;
    private Integer txWithOpReturn;

    private String json;

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Integer getBlocksWithOpReturn() {
        return blocksWithOpReturn;
    }

    public void setBlocksWithOpReturn(Integer blocksWithOpReturn) {
        this.blocksWithOpReturn = blocksWithOpReturn;
    }

    public Integer getTxWithOpReturn() {
        return txWithOpReturn;
    }

    public void setTxWithOpReturn(Integer txWithOpReturn) {
        this.txWithOpReturn = txWithOpReturn;
    }
}
