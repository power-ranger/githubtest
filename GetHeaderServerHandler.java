package HeaderServer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;


public class GetHeaderServerHandler extends SimpleChannelHandler{
	
	private static final Logger logger =
			Logger.getLogger(GetHeaderServerHandler.class.getName());
	
	@Override
	public void messageReceived(
			ChannelHandlerContext ctx, MessageEvent e) {
		ChannelBuffer buf = (ChannelBuffer) e.getMessage();
		ChannelFuture f = e.getChannel().write(buf); // Print to Browser
		System.out.println(buf.toString()); // Print Channel Buffer
		f.addListener(ChannelFutureListener.CLOSE);
	}
	
	@Override
	public void exceptionCaught(
			ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.log(Level.WARNING,"Error from downstream.",e.getCause());
		e.getChannel().close();
	}
}
