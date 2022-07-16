package org.dvlyyon.net.odl;

import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dvlyyon.common.net.ContextInfoException;
import org.dvlyyon.common.util.ThreadUtils;
import org.dvlyyon.common.util.XMLUtils;
import org.dvlyyon.net.http1.HttpClientFactory;
import org.dvlyyon.net.http1.HttpClientInf;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class OdlRestconfClientImpl implements OdlRestconfClientInf {
	Properties context = null;
	HttpClientInf httpClient = null;
	final static Pattern connectStatusPattern = 
			Pattern.compile("^<connection-status[^>]*>(.*)</connection-status>$");

	static Namespace netTopoNS = Namespace.getNamespace("urn:TBD:params:xml:ns:yang:network-topology");
	static Namespace odlTopoNS = Namespace.getNamespace("urn:opendaylight:netconf-node-topology");

	String configMountPoint = "restconf/config/network-topology:network-topology/topology/topology-netconf/node";
	String operMountPoint   = "restconf/operations/network-topology:network-topology/topology/topology-netconf/node";
	String getMountPoint    = "restconf/operational/network-topology:network-topology/topology/topology-netconf/node";
	
	StringBuilder sb = new StringBuilder();
	
	private final static Log log = LogFactory.getLog(OdlRestconfClientImpl.class);
	
	private void checkContext() throws ContextInfoException {
		ContextInfoException.checkKey(context, WEBSERVER);
		ContextInfoException.checkKey(context, WEBPORT);
		ContextInfoException.checkKey(context, WEBUSER);
		ContextInfoException.checkKey(context, WEBPASSWD);
		ContextInfoException.checkKey(context, NODEIP);
		ContextInfoException.checkKey(context, NODEID);
		ContextInfoException.checkKey(context, NODEPORT);
		ContextInfoException.checkKey(context, NODEUSER);
		ContextInfoException.checkKey(context, NODEPASSWD);
	}
	
	@Override
	public void setContext(Properties context) throws ContextInfoException {
		this.context = context;
		checkContext();
	}
	
	private String getNodeConnectionInfo() {
		
		Element nodeE = new Element("node",netTopoNS);
		Element elem = new Element("node-id",netTopoNS);
		elem.setText(context.getProperty(NODEID));
		nodeE.addContent(elem);
		elem = new Element("host", odlTopoNS);
		elem.setText(context.getProperty(NODEIP));
		nodeE.addContent(elem);
		elem = new Element("port",odlTopoNS);
		elem.setText(context.getProperty(NODEPORT));
		nodeE.addContent(elem);
		elem = new Element("username",odlTopoNS);
		elem.setText(context.getProperty(NODEUSER));
		nodeE.addContent(elem);
		elem = new Element("password",odlTopoNS);
		elem.setText(context.getProperty(NODEPASSWD));
		nodeE.addContent(elem);
		elem = new Element("tcp-only",odlTopoNS);
		elem.setText("false");
		nodeE.addContent(elem);
		elem = new Element("keepalive-delay",odlTopoNS);
		elem.setText("0");
		nodeE.addContent(elem);
		return XMLUtils.toXmlString(nodeE,true);
	}

	private void mountPoint(StringBuilder sb) {
		mountPoint(sb,this.configMountPoint);
	}
	
	private void mountPoint(StringBuilder sb, String mountPoint) {
		sb.delete(0, sb.length());
		sb.append("http://").append(context.getProperty(WEBSERVER)).append(":").
			append(context.getProperty(WEBPORT)).append("/").
			append(mountPoint).append("/").append(context.getProperty(NODEID)).append("/");
	}

	private void nodeRoot(StringBuilder sb) {
		nodeRoot(sb,this.configMountPoint);
	}

	private void nodeRoot(StringBuilder sb, String mountPoint) {
		mountPoint(sb, mountPoint);
		sb.append("yang-ext:mount").append("/");
	}
	
	private boolean isConnected(String response) {
		if (response == null) return false;
		String [] lines = response.split("\n");
		for (String line:lines) {
			line = line.trim();
			Matcher m = connectStatusPattern.matcher(line);
			if (m.matches()) {
				String r = m.group(1);
				if (r.trim().equals("connected"))
					return true;
				else
					return false;
			}
		}
		return false;
	}
	private void getCapability() throws Exception {
		mountPoint(sb,getMountPoint);
		String uri = sb.toString();
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Accept", "application/xml");
		String response = httpClient.get(uri, properties);
		int i = 0;
		while (!isConnected(response) && i++ <20) {
			ThreadUtils.sleep(5);
			response = httpClient.get(uri, properties);
		}
		if (!isConnected(response)) {
			throw new Exception ("NOT connected within 100s \n"+ response);
		}
	}

	private void connectNode() throws Exception {
		mountPoint(sb);
		String uri    = sb.toString();
		String entity = getNodeConnectionInfo();
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		httpClient.put(uri, entity, properties);
	}
	
	private String getFullUri(String path) {
		return getFullUri(path,this.configMountPoint);
	}

	private String getFullUri(String path, String mountPoint) {
		nodeRoot(sb, mountPoint);
		sb.append(path);
		log.debug("Full URI:"+sb.toString());
		return sb.toString();
	}
	
	public void closeNodeConnection() throws Exception{
		sb.delete(0, sb.length());
		mountPoint(sb);
		String uri = sb.toString();
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		httpClient.delete(uri, properties);
	}

	@Override
	public void login() throws Exception {
		if (context == null) throw new ContextInfoException("Please set context before calling this method");
		httpClient = HttpClientFactory.get(null);
		httpClient.setCredential(context.getProperty(WEBUSER), 
				context.getProperty(WEBPASSWD), 
				context.getProperty(WEBSERVER), 
				Integer.parseInt(context.getProperty(WEBPORT)));
		String uri = "http://"+context.getProperty(WEBSERVER)+
				":"+context.getProperty(WEBPORT)+
				"/apidoc/explorer/index.html";
		log.debug("URI:-->"+uri+" with user:"+context.getProperty(WEBUSER)+"@"+context.getProperty(WEBPASSWD));
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		httpClient.connect(uri, properties);
		log.info("Success to connect -->"+uri);
		connectNode();
		getCapability();
	}

	@Override
	public void close() {
		try {
			this.closeNodeConnection();
		} catch (Exception e) {
			log.error("Exception when closing node connect", e);
		} finally {
			httpClient.close();
		}
	}

	@Override
	public String get(String uri) throws Exception {
		return get(uri,false);
	}
	
	public String get(String uri, boolean fullPath) throws Exception{
		if (!fullPath) {
			uri = this.getFullUri(uri,getMountPoint);
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		return httpClient.get(uri, properties);
	}

	@Override
	public String add(String uri, String content) throws Exception {
		return add(uri,content,false);
	}

	public String add(String uri, String content, boolean fullPath) throws Exception {
		if (!fullPath) {
			uri = this.getFullUri(uri);
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		return httpClient.post(sb.toString(), content, properties);
	}

	@Override
	public String set(String uri, String content) throws Exception {		
		return set(uri,content,false);
	}

	public String set(String uri, String content, boolean fullPath) throws Exception {
		if (!fullPath) {
			uri = this.getFullUri(uri);
		}		
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		return httpClient.put(uri, content, properties);
	}
	
	@Override
	public String delete(String uri) throws Exception {
		return delete(uri,false);
	}
	
	public String delete(String uri, boolean fullPath) throws Exception {
		if (!fullPath) {
			uri = this.getFullUri(uri);
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		return httpClient.delete(uri, properties);
	}	

	@Override
	public String callRpc(String uri, String content) throws Exception {
		return callRpc(uri,content,false);
	}

	public String callRpc(String uri, String content, boolean fullPath) throws Exception {
		if (!fullPath) {
			uri = this.getFullUri(uri,this.operMountPoint);
		}
		TreeMap<String,String> properties = new TreeMap<String,String>();
		properties.put("Content-Type", "application/xml");
		properties.put("Accept", "application/xml");
		return httpClient.post(uri, content, properties);
	}	
}
