package io.geobit.opreturn;

import com.google.common.base.Optional;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.geobit.opreturn.entity.CoinSecret;


/**
 * Created by Riccardo Casatta @RCasatta on 07/04/15.
 */
public class CoinSecretIndex extends HttpServlet {
    private final static Logger log  = Logger.getLogger(CoinSecretIndex.class.getName());

    private CoinSecretMemory coinsecretMemory     = new CoinSecretMemory();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final String height = req.getParameter("height");
            if(height==null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Optional<CoinSecret> coinSecret = coinsecretMemory.get(height);
            resp.setContentType("text/plain");
            resp.getWriter().write(coinSecret.get().getJson());
        } catch (Exception e) {
            log.warning(e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}