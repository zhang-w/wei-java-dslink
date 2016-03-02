package org.dsa.iot.wei;

import java.util.concurrent.CountDownLatch;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dsa.iot.wei.simulators.SimGatewayDevice0;

/**
 * The main class that starts the DSLink and list everything from root.
 */
public class Main extends DSLinkHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private DSLinkProvider provider;
  private CountDownLatch latch;

  @Override
  public void preInit() {
      // Latch is used to ensure responder is initialized first
      latch = new CountDownLatch(1);
  }
  
	@Override
	public boolean isRequester() {
		return true;
	}
	
  @Override
  public boolean isResponder() {
      return true;
  }

	@Override
	public void onRequesterConnected(final DSLink link) {
		LOGGER.info("--------------");
		LOGGER.info("Connected!");
		
		subscribe(link);
	}

	@Override
	public void onRequesterDisconnected(DSLink link) {
		LOGGER.info("Disconnected with broker.");
	}
	
  @Override
  public void onResponderInitialized(DSLink link) {
		  SimGatewayDevice0.init(link);
      LOGGER.info("Responder initialized");
      latch.countDown();
  }

	public static void main(String[] args) {
		Main main = new Main();
		main.provider = DSLinkFactory.generate(args, main);
		if (main.provider == null) {
			return;
		}
		main.provider.start();
	}


	/**
	 * Subscribes to the simGatewayDevice0 node and display it from DgLux.
	 *
	 * @param link
	 *          Requester link used to communicate to the endpoint.
	 */
	private static void subscribe(DSLink link) {
		String path = link.getPath() + "/" + SimGatewayDevice0.NAME + "/event";
		LOGGER.info("Subscribing {} ...", path);
		link.getRequester().subscribe(path, new Handler<SubscriptionValue>() {
			@Override
			public void handle(SubscriptionValue event) {
				int val = event.getValue().getNumber().intValue();
				LOGGER.info("Received new event: {}", val);
			}
		});
		LOGGER.info("Subscribed {}.", path);
	}
}
