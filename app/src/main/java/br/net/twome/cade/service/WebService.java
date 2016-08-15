package br.net.twome.cade.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import br.net.twome.cade.Usuario;

public class WebService {

    private static final String BASE_URL = "http://cloud.2me.net.br/kdtodomundo/api/usuario";

    public static Usuario login(Usuario usuario) throws Exception {
        String jsonStr = getUrl("POST", usuario);
        return createBean(new JSONObject(jsonStr));
    }

    public static List<Usuario> enviaLocalizacao(Usuario usuario) throws Exception {
        String jsonStr = getUrl("PUT", usuario);
        ArrayList<Usuario> list;
        if(jsonStr.trim().startsWith("[")) {
            JSONArray result = new JSONArray(jsonStr);
            list = new ArrayList<>(result.length());
            for (int i = 0; i < result.length(); i++) {
                list.add(createBean(result.getJSONObject(i)));
            }
        }else{
            list = new ArrayList<>(1);
            list.add(createBean(new JSONObject(jsonStr)));
        }
        return list;
    }

    private static Usuario createBean(JSONObject obj) throws Exception {
        if (obj.has("error")) {
            throw new IllegalStateException(obj.getString("error"));
        }
        Usuario usuario = new Usuario();
        usuario.setNickname(obj.getString("nickname"));
        if (obj.has("latitude")) {
            usuario.setLatitude(obj.getDouble("latitude"));
        }
        if (obj.has("longitude")) {
            usuario.setLongitude(obj.getDouble("longitude"));
        }
        return usuario;
    }

    private static String getUrl(String method, Usuario usuario) throws Exception {

        URL url = new URL(BASE_URL);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setConnectTimeout(15000);
        http.setRequestMethod(method);
        http.setDoOutput(true);
        http.setRequestProperty("Content-Type", "application/json");

        JSONObject obj = new JSONObject();
        obj.put("nickname", usuario.getNickname());
        obj.put("senha", usuario.getSenha());
        obj.put("latitude", usuario.getLatitude());
        obj.put("longitude", usuario.getLongitude());

        OutputStream out = http.getOutputStream();
        out.write(obj.toString().getBytes(Charset.forName("utf-8")));
        out.flush();
        out.close();

        StringBuilder result = new StringBuilder();
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(http.getInputStream(), Charset.forName("utf-8")));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }finally {
            if (rd != null) {
                rd.close();
            }
        }
        return result.toString();
    }

}
