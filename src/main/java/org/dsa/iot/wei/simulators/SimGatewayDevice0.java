package org.dsa.iot.wei.simulators;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.Writable;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.actions.table.Row;
import org.dsa.iot.dslink.node.actions.table.Table;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValuePair;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.Objects;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 *
 */

public class SimGatewayDevice0 {

	public static final String	NAME	 = "simGatewayDevice0";
	private static final Logger	LOGGER = LoggerFactory.getLogger(SimGatewayDevice0.class);
	private static final Random	RANDOM = new Random();
	private static String EVENT_PATH = "/";

	/**
	 * Initializes the responder link.
	 *
	 * @param link
	 *          Responder link to initialize.
	 */
	public static void init(DSLink link) {
		NodeBuilder builder = link.getNodeManager().createRootNode(NAME);
		Node node = builder.build();

		initSettableNode(node);
		initEventNode(node);
		initActionNode(node);
		LOGGER.info("Simulator {} initialized.", NAME);
	}

	/**
	 * Initializes the 'settable' node. By default this node will have a value of
	 * 'UNSET' until it is set by the requester.
	 *
	 * @param node
	 *          Values node.
	 * @see org.dsa.iot.dual.requester.Requester#setNodeValue
	 */
	private static void initSettableNode(Node node) {
		NodeBuilder builder = node.createChild("settable");
		builder.setWritable(Writable.WRITE);
		builder.setValueType(ValueType.STRING);
		builder.setValue(new Value("UNSET"));
		builder.getListener().setValueHandler(new Handler<ValuePair>() {
			@Override
			public void handle(ValuePair event) {
				String val = event.getCurrent().getString();
				LOGGER.info("Responder has a new value set from requester: {}", val);
				LOGGER.info("External source? " + event.isFromExternalSource());
			}
		});
		node = builder.build();
		node.setSerializable(false);
		LOGGER.info("Responder has a current value of {}", node.getValue().toString());
	}

	/**
	 * Initializes the 'event' node which hold events from this device. This node
	 * can be subscribed to in order to get events generated by this device.
	 *
	 * @param node
	 *          Values node.
	 * @see org.dsa.iot.dual.requester.Requester#subscribe
	 */
	private static void initEventNode(Node node) {
		NodeBuilder builder = node.createChild("event");
		final Node child = builder.build();
		child.setValueType(ValueType.DYNAMIC);
		
		EVENT_PATH = child.getPath();
		LOGGER.info("EVENT_PATH: {}", EVENT_PATH);

    Objects.getDaemonThreadPool().scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
          Value val = new Value(RANDOM.nextInt());
          val.setSerializable(false);
          child.setValue(val);
      }
    }, 0, 5, TimeUnit.SECONDS);
	}

	/**
	 * Initializes the 'action' node that can be invoked.
	 *
	 * @param node
	 *          Values node.
	 * @see org.dsa.iot.dual.requester.Requester#invoke
	 */
	private static void initActionNode(Node node) {
		NodeBuilder builder = node.createChild("turnOff");
		builder.setAction(new Action(Permission.READ, new Handler<ActionResult>() {
			@Override
			public void handle(ActionResult event) {
				LOGGER.info("Responder action turnOff invoked from requester");
				Table t = event.getTable();
				t.addRow(Row.make(new Value("Turned Off")));
			}
		}).addResult(new Parameter("response", ValueType.STRING)));
		builder.build();
	}
}

