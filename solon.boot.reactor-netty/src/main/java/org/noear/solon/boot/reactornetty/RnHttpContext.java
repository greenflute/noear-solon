package org.noear.solon.boot.reactornetty;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import org.noear.solon.XUtil;
import org.noear.solon.core.XContext;
import org.noear.solon.core.XFile;
import org.noear.solon.core.XMap;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;


public class RnHttpContext extends XContext {
    private final HttpServerRequest _request;
    private final HttpServerResponse _response;
    private final HttpRequestParser _request_parse;
    public RnHttpContext(HttpServerRequest request, HttpServerResponse response, HttpRequestParser request_parse) {
        _request = request;
        _response = response;

        try {
            _request_parse = request_parse.parse();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object request() {
        return _request;
    }

    private String _ip;
    @Override
    public String ip() {
        if(_ip == null) {
            _ip = header("X-Forwarded-For");
            if (_ip == null) {
                InetSocketAddress insocket =_request.remoteAddress();
                _ip = insocket.getAddress().getHostAddress();
            }
        }

        return _ip;
    }

    @Override
    public String method() {
        return _request.method().name();
    }

    @Override
    public String protocol() {
        return _request.version().protocolName();
    }

    private URI _uri;
    @Override
    public URI uri() {
        if(_uri == null){
            _uri = URI.create(url());
        }
        return _uri;
    }

    @Override
    public String path() {
        return uri().getPath();
    }

    private String _url;
    @Override
    public String url() {
        if (_url == null) {
            _url = _request.uri();

            if (_url != null && _url.startsWith("/")) {
                _url = _request.scheme()+"://" + header("Host") + _url;
            }
        }

        return _url;
    }

    @Override
    public long contentLength() {
        return _request.receiveContent().count().block();
    }

    @Override
    public String contentType() {
        return header("Content-Type");
    }

    @Override
    public String body() throws IOException {
        return _request.receiveContent().blockFirst().content().toString();
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return new ByteArrayInputStream(_request.receiveContent().blockFirst().content().array());
    }

    @Override
    public String[] paramValues(String key) {
        List<String> list = paramsMap().get(key);
        if(list == null){
            return null;
        }

        return list.toArray(new String[list.size()]);
    }

    @Override
    public String param(String key) {
        //要充许为字符串
        //默认不能为null
        return paramMap().get(key);
    }

    @Override
    public String param(String key, String def) {
        String tmp = paramMap().get(key);
        if(tmp == null) {
            return def;
        }else{
            return tmp;
        }
    }

    private XMap _paramMap;
    @Override
    public XMap paramMap() {
        if(_paramMap == null){
            _paramMap = new XMap();

            _request_parse.parmMap.forEach((k,l)->{
                if(l.size() > 0){
                    _paramMap.put(k,l.get(0));
                }
            });
        }

        return _paramMap;
    }

    @Override
    public Map<String, List<String>> paramsMap() {
        return _request_parse.parmMap;
    }

    @Override
    public List<XFile> files(String key) throws Exception {
        return _request_parse.fileMap.get(key);
    }

    @Override
    public String cookie(String key) {
        return cookieMap().get(key);
    }

    @Override
    public String cookie(String key, String def) {
        String temp = cookieMap().get(key);
        if(temp == null){
            return  def;
        }else{
            return temp;
        }
    }

    private XMap _cookieMap;
    @Override
    public XMap cookieMap() {
        if(_cookieMap == null){
            _cookieMap = new XMap();

            String _cookieMapStr = _request.requestHeaders().get(COOKIE);
            if (_cookieMapStr != null) {
                Set<Cookie> tmp = ServerCookieDecoder.LAX.decode(_cookieMapStr);

                for(Cookie c1 :tmp){
                    _cookieMap.put(c1.name(),c1.value());
                }
            }
        }

        return _cookieMap;
    }

    @Override
    public String header(String key) {
        return headerMap().get(key);
    }

    @Override
    public String header(String key, String def) {
        String tmp = header(key);
        if(tmp == null){
            return def;
        }else{
            return  tmp;
        }
    }

    private XMap _headerMap;
    @Override
    public XMap headerMap() {
        if(_headerMap == null) {
            _headerMap = new XMap();
            HttpHeaders headers = _request.requestHeaders();

            for(Map.Entry<String, String> kv : headers){
                _headerMap.put(kv.getKey(), kv.getValue());
            }
        }

        return _headerMap;
    }

    @Override
    public Object response() {
        return _response;
    }

    @Override
    public void charset(String charset) {
        _charset = charset;
    }
    private String _charset = "UTF-8";

    @Override
    protected void contentTypeDoSet(String contentType) {
        if (_charset != null) {
            if (contentType.indexOf(";") < 0) {
                headerSet("Content-Type", contentType + ";charset=" + _charset);
                return;
            }
        }

        headerSet("Content-Type", contentType);
    }

    @Override
    public OutputStream outputStream() throws IOException{
        return _outputStream;
    }

    @Override
    public void output(String str) {
        try {
            OutputStream out = _outputStream;

            out.write(str.getBytes(_charset));
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void output(InputStream stream) {
        try {
            OutputStream out = _outputStream;

            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = stream.read(buff, 0, 100)) > 0) {
                out.write(buff, 0, rc);
            }
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    protected ByteBufOutputStream _outputStream = new ByteBufOutputStream(Unpooled.buffer());

    @Override
    public void headerSet(String key, String val) {
        _response.header(key,val);
    }

    @Override
    public void cookieSet(String key, String val, String domain, String path, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(val).append(";");

        if (XUtil.isNotEmpty(path)) {
            sb.append("path=").append(path).append(";");
        }

        if (maxAge >= 0) {
            sb.append("max-age=").append(maxAge).append(";");
        }

        if (XUtil.isNotEmpty(domain)) {
            sb.append("domain=").append(domain.toLowerCase()).append(";");
        }

        _response.addHeader(SET_COOKIE, sb.toString());
    }

    @Override
    public void redirect(String url) {
        redirect(url,302);
    }

    @Override
    public void redirect(String url, int code) {
        try {
            headerSet("Location", url);
            status(code);
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int status() {
        return _status;
    }

    private int _status = 200;
    @Override
    public void status(int status) {
        _status = status;
        _response.status(status);
    }
}
