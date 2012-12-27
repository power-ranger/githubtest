package WebSocket;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;


public class WebSocketServerHandler extends SimpleChannelUpstreamHandler{
	
	private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocketServerHandler.class);
	
	private static final String PATH = "/websocket";
	
	private WebSocketServerHandshaker handshaker;
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e){
		Object msg = e.getMessage();
		if(msg instanceof HttpRequest){
			handleHttpRequest(ctx, (HttpRequest) msg);
		}else if(msg instanceof WebSocketFrame){
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}
	
	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req){
		if(req.getMethod() != HttpMethod.GET){
			sendHttpResponse(ctx, req, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
		}
		
        if(req.getUri().equals("/")){
        	HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        	
        	ChannelBuffer content = WebSocketServerIndexPage.getContent(getWebSocketLocation(req));
        	
        	res.setHeader(HttpHeaders.Names.HOST, "text/html; charset=UTF-8");
        	
        	HttpHeaders.setContentLength(res, content.readableBytes());
        	
        	res.setContent(content);
        	sendHttpResponse(ctx, req, res);
        	return;
        } else if(req.getUri().equals("/favicon.ico")){
       	 HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
       	 sendHttpResponse(ctx, req, res);
       	 return;
       }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
        		 getWebSocketLocation(req), null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
        	wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        }else
        {
        	handshaker.handshake(ctx.getChannel(), req).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
        }
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
			return;
		} else if (frame instanceof PingWebSocketFrame) {
			ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
			return;
		}else if (!(frame instanceof TextWebSocketFrame)) {
			 throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().
					 getName()));
		}
		
		 String request = ((TextWebSocketFrame) frame).getText();
		 if (logger.isDebugEnabled()) {
			 logger.debug(String.format("Channel %s received %s", ctx.getChannel().getId(), request));
		 }
		 ctx.getChannel().write(new TextWebSocketFrame(request.toUpperCase()));
	}
	
	@SuppressWarnings("deprecation")
	private static void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res){
		if(res.getStatus().getCode() != 200){
			res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
		}
		
		ChannelFuture f = ctx.getChannel().write(res);
		if (!res.isKeepAlive() || res.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		 e.getCause().printStackTrace();
		 e.getChannel().close();
	}
	private static String getWebSocketLocation(HttpRequest req) {
		return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + PATH;
	}
	
}
