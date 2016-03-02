package com.ibm.iot.analytics.edge;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.dsa.iot.dslink.methods.requests.ListRequest;
import org.dsa.iot.dslink.methods.responses.ListResponse;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.SubscriptionValue;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.util.StringUtils;
import org.dsa.iot.dslink.util.handler.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.iot.analytics.edge.simulators.SimGatewayDevice0;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * The main class that starts the DSLink and list everything from root.
 */
public class Main extends DSLinkHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private DSLinkProvider provider;
	private DSLink				 link;

	@Override
	public boolean isRequester() {
		return true;
	}

	@Override
	public void onRequesterConnected(final DSLink link) {
		this.link = link;
		LOGGER.info("--------------");
		LOGGER.info("Connected!");
		/// ListRequest request = new ListRequest("/");
		// link.getRequester().list(request, new Lister(request));
		SimGatewayDevice0.init(link);
		
		subscribe(link);
	}

	@Override
	public void onRequesterDisconnected(DSLink link) {
		LOGGER.info("Disconnected with broker.");
	}

	public static void main(String[] args) {
		Main main = new Main();
		main.provider = DSLinkFactory.generate(args, main);
		if (main.provider == null) {
			return;
		}
		main.provider.start();
		main.provider.sleep();
	}

	private class Lister implements Handler<ListResponse> {
		private final ListRequest request;
		public Lister(ListRequest request) {
			this.request = request;
		}

		@Override
		public void handle(ListResponse resp) {
			StringBuilder msg = new StringBuilder("\n");
			msg.append("Received response: ");
			msg.append(request.getName());
			msg.append('\n');
			msg.append(" - Path: ");
			msg.append(request.getPath());
			msg.append('\n');
			Node node = resp.getNode();
			msg.append(" - Profile: ");
			msg.append(node.getProfile());
			msg.append('\n');
			msg.append(printValueMap(node.getAttributes(), "- Attribute", " "));
			msg.append(printValueMap(node.getConfigurations(), "- Configuration", " "));
			{
				msg.append(" - Is action? ");
				msg.append(node.getAction() != null);
				msg.append('\n');
			}
			{
				msg.append(" - Is metric? ");
				msg.append(node.getValueType() != null);
				msg.append('\n');
			}

			Map<Node, Boolean> nodes = resp.getUpdates();
			if (nodes != null && nodes.size() > 0) {
				msg.append(" Children: \n");
				for (Map.Entry<Node, Boolean> entry : nodes.entrySet()) {
					Node child = entry.getKey();
					boolean removed = entry.getValue();
					String spaces = "    ";
					msg.append(spaces);
					msg.append("- Name: ");
					msg.append(child.getName());
					msg.append('\n');
					spaces += "  ";
					msg.append(spaces);
					msg.append("- Profile: ");
					msg.append(child.getProfile());
					msg.append('\n');
					Set<String> interfaces = child.getInterfaces();
					if (interfaces != null) {
						msg.append(spaces);
						msg.append("- Interfaces: ");
						msg.append(StringUtils.join(child.getInterfaces(), "|"));
						msg.append('\n');
					}
					msg.append(spaces);
					msg.append("- Removed: ");
					msg.append(removed);
					msg.append('\n');

					if (removed) {
						continue;
					}

					msg.append(printValueMap(child.getAttributes(), "- Attribute", spaces));
					msg.append(printValueMap(child.getConfigurations(), "- Configuration", spaces));

					ListRequest newReq = new ListRequest(child.getPath());
					link.getRequester().list(newReq, new Lister(newReq));
				}
			}

			LOGGER.info(msg.toString());
			System.out.flush();
		}

		private String printValueMap(Map<String, Value> map, String name, String indent) {
			StringBuilder msg = new StringBuilder();
			if (map != null) {
				for (Map.Entry<String, Value> conf : map.entrySet()) {
					String a = conf.getKey();
					String v = conf.getValue().toString();
					if (indent != null) {
						msg.append(indent);
					}
					msg.append(name);
					msg.append(": ");
					msg.append(a);
					msg.append(" => ");
					msg.append(v);
					msg.append('\n');
				}
			}
			return msg.toString();
		}
	}

	/**
	 * Subscribes to the simGatewayDevice0 node and display it from DgLux.
	 *
	 * @param link
	 *          Requester link used to communicate to the endpoint.
	 */
	private static void subscribe(DSLink link) {
		// String path = link.getPath() + "/" + SimGatewayDevice0.NAME + "/event";
		String path = "/downstream/dual/values/dynamic";
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