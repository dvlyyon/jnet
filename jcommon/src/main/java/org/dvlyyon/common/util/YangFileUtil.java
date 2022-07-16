package org.dvlyyon.common.util;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


public class YangFileUtil {

	public class Yang {
		Map<String, Module> modules;
		
		public Yang() {
			modules = new HashMap<String, Module>();
		}

		public class Leaf {
			String name;
			Type type;
			String extraInfo = null;


			public Leaf(String name) {
				this.name = name;
			}
			public void setType(Type type) {
				this.type = type;
			}
		}

		public class LeafList {
			String name;
			Type type;
			String extraInfo = null;


			public LeafList(String name) {
				this.name = name;
			}
			public void setType(Type type) {
				this.type = type;
			}
		}

		public class Type {
			String name;
			String base;

			public Type(String name) {
				this.name = name;
			}
		}

		public class Group {
			String name;
			Map<String, Leaf> leaves;
			Map<String, ContainerNode> containers;
			List<String> uses;

			public Group(String grpName) {
				this.name = grpName;
				leaves = new HashMap<String, Leaf>();
				uses = new ArrayList<String>();
				containers = new HashMap<String,ContainerNode>();
			}

			public void addLeaf(Leaf leaf) {
				String name = leaf.name;
				if (leaves.get(name) != null) {
					System.out.println("duplicate leaf name "+name);
				}
				leaves.put(name, leaf);

			}

			public void addUses(String useName) {
				uses.add(useName);				
			}

			public void addContainer(ContainerNode container) {
				containers.put(container.name, container);			
			}

		}

		public class Identity {
			String name;
			String parent;
			List<String> children;
			public Identity(String name) {
				this.name = name;
				children = new ArrayList<String>();
			}

			public void addChild(String child) {
				children.add(child);
			}

			public void parent(String parent) {
				this.parent = parent;
			}
		}

		public class YangNode {

		}

		public class ContainerNode extends YangNode {
			String name;
			Map<String, Leaf> leaves;
			Map<String, ContainerNode> containers;
			Map<String, ListNode> lists;
			List<String> uses;

			public ContainerNode(String grpName) {
				this.name = grpName;
				leaves = new HashMap<String, Leaf>();
				lists  = new HashMap<String, ListNode>();
				uses = new ArrayList<String>();
				containers = new HashMap<String, ContainerNode>();
			}

			public void addUses(String uses2) {
				uses.add(uses2);
			}

			public void addLeaf(Leaf leaf) {
				leaves.put(leaf.name,leaf);
			}

			public void addList(ListNode list) {
				lists.put(list.name, list);
			}

			public void addContainer(ContainerNode container) {
				containers.put(container.name, container);
			}

			public void toXML(Map<String, Module> modules, Module localModule, StringBuilder sb) {
				if (uses.size()>0) {
					
				}
				if (!leaves.isEmpty()) {
					leaves.forEach((k,v)->{
						
					});
				}
			}
		}

		public class ListNode extends YangNode {
			String name;
			Map<String, Leaf> leaves;
			Map<String, LeafList> leafLists;
			Map<String, ListNode> lists;
			Map<String, ContainerNode> containers;
			List<String> uses;
			List<String> keys;

			public ListNode(String grpName) {
				this.name = grpName;
				leaves = new HashMap<String, Leaf>();
				leafLists = new HashMap<String, LeafList>();
				containers = new HashMap<String, ContainerNode>();
				lists = new HashMap<String, ListNode>();
				uses = new ArrayList<String>();
				keys = new ArrayList<String>();
			}

			public void addUses(String uses2) {
				uses.add(uses2);
			}

			public void addLeaf(Leaf leaf) {
				leaves.put(leaf.name,leaf);

			}

			public void addKey(String key) {
				keys.add(key);
			}

			public void addLeafList(LeafList leafList) {
				leafLists.put(leafList.name, leafList);

			}

			public void addList(ListNode list) {
				lists.put(list.name,list);

			}

			public void addContainer(ContainerNode container) {
				containers.put(container.name, container);
				
			}

		}

		public class Module {
			String name;
			String prefix;
			String namespace;
			Map<String, Identity> identityMap;
			Map<String, Group> groupMap;
			Map<String, ContainerNode> containerMap;

			public Module (String name, String prefix, String namespace) {
				this.name = name;
				this.prefix = prefix;
				this.namespace = namespace;
				identityMap = new HashMap<String, Identity>();
				groupMap = new HashMap<String, Group>();
				containerMap = new HashMap<String, ContainerNode>();
			}

			public Identity getIdentity(String id) {
				return identityMap.get(id);
			}

			public void addIdentity(String name, Identity identity) {
				identityMap.put(name, identity);

			}

			public void addContainer(ContainerNode cn) {
				containerMap.put(cn.name, cn);				
			}

		}

		public void parseIdentity(Module module, Element node) {
			String name = node.attributeValue("name");
			Identity identity = new Identity(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch(elemType) {
				case "description":
					break;
				case "base":
					String parent = v.attributeValue("name");
					Identity parentID = module.getIdentity(parent);
					if (parentID == null) {
						System.out.println("Cannot get base identity " + parent + " for identity " + name);
						System.exit(1);
					}
					parentID.addChild(name);
					identity.parent(parent);
					break;
				default:
					System.out.println("unexpect child:>"+elemType);
					System.exit(1);
				}
			});
			module.addIdentity(name, identity);
		}

		public Leaf parseLeaf(Element node) {
			String name = node.attributeValue("name");
			Leaf l = new Leaf(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "type":
					Type type = parseType(v);
					l.setType(type);
					break;
				case "description":
				case "mandatory":
				case "default":
					break;
				default:
					System.out.println("leaf "+ name + " include other sub-child:"+v.getName());
					System.exit(1);	
				}
			});
			return l;
		}

		public Type parseType(Element node) {
			String name = node.attributeValue("name");
			System.out.println("----Type: "+ name + "----");
			Type t = new Type(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "base":
					String base = v.attributeValue("name");
					t.base = base;
					break;
				case "description":
					break;
				default:
					System.out.println("leaf "+ name + " include other sub-child:"+v.getName());
					System.exit(1);	
				}
			});
			return t;

		}

		public LeafList parseLeafList(Element node) {
			String name = node.attributeValue("name");
			LeafList ll = new LeafList(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "type":
					Type type = parseType(v);
					ll.setType(type);
					break;
				case "description":
					break;
				default:
					System.out.println("leaf "+ name + " include other sub-child:"+v.getName());
					System.exit(1);	
				}
			});
			return ll;
		}

		public String parseUses(Element node) {
			String name = node.attributeValue("name");
			List<Element> elements = node.elements();
			elements.forEach(v->{
				String elemType = v.getName();
				System.out.println("uses "+name+" include other sub-elements:\n" + v.asXML());
			});
			return name;
		}

		public void parseGroup(Module module, Element node) {
			String grpName = node.attributeValue("name");
			Group group = new Group(grpName);
			List<Element> children = node.elements(); 
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "leaf":
					Leaf leaf = parseLeaf(v);
					group.addLeaf(leaf);
					break;
				case "uses":
					String uses = parseUses(v);
					group.addUses(uses);
					break;
				case "description":
					break;
				case "container":
					ContainerNode container = parseContainer(v);
					group.addContainer(container);
					break;
				default:
					System.out.println("no expected element:"+elemType + " in grouping "+grpName);
					System.exit(1);
				}
			});
		}

		private ContainerNode parseContainer(Element node) {
			String name = node.attributeValue("name");
			ContainerNode container = new ContainerNode(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "description":
				case "presence":
				case "when":
				case "must":
					break;
				case "uses":
					String uses = parseUses(v);
					container.addUses(uses);
					break;
				case "leaf":
					Leaf leaf = parseLeaf(v);
					container.addLeaf(leaf);
					break;
				case "list":
					ListNode list = parseList(v);
					container.addList(list);
					break;
				case "container":
					ContainerNode ccontainer = parseContainer(v);
					container.addContainer(ccontainer);
					break;
				default:
					System.out.println("no expected element:"+elemType + " in container " + name);
					System.exit(1);
				}
			});
			return container;
		}

		private ListNode parseList(Element node) {
			String name = node.attributeValue("name");
			ListNode list = new ListNode(name);
			List<Element> children = node.elements();
			children.forEach(v->{
				String elemType = v.getName();
				switch (elemType) {
				case "description":
				case "when":
				case "must":
				case "presence":
				case "ordered-by":
					break;
				case "key":
					String key = v.attributeValue("value");
					list.addKey(key);
					break;
				case "uses":
					String uses = parseUses(v);
					list.addUses(uses);
					break;
				case "leaf":
					Leaf leaf = parseLeaf(v);
					list.addLeaf(leaf);
					break;
				case "leaf-list":
					LeafList leafList = parseLeafList(v);
					list.addLeafList(leafList);
					break;
				case "list":
					ListNode sublist = parseList(v);
					list.addList(sublist);
					break;
				case "container":
					ContainerNode container = parseContainer(v);
					list.addContainer(container);
					break;
				default:
					System.out.println("no expected element:"+elemType + " in list "+name);
					System.exit(1);
				}
			});
			return list;
		}

		public void parse(Element node) {
			String docType = node.getName();
			if (docType != "module") {
				System.out.println("Only module file is supported now");
			}
			String moduleName = node.attributeValue("name");
			List<Element> prefixList = node.elements("prefix");
			String prefix = node.element("prefix").attributeValue("value");
			String namespace = ((Element)node.elements("namespace").get(0)).attributeValue("uri");
			Module module = new Module(moduleName,prefix, namespace);
			List<Element> elements = node.elements();
			elements.forEach(e->{
				String name = e.getName();
				switch(name) {
				case "namespace":
				case "prefix":
				case "description":
				case "organization":
				case "contact":
				case "revision":
					System.out.println("ignore:"+name+" in module");
					break;
				case "identity":
					parseIdentity(module,e);
					break;
				case "grouping":
					parseGroup(module,e);
					break;
				case "container":
					ContainerNode cn = parseContainer(e);
					module.addContainer(cn);
					break;
				default:
					System.out.println("module " + moduleName + " include other element: " + name);
					System.exit(1);				
				}
			});
			modules.put(moduleName, module);
		}
		
		public void toXML() {
			StringBuilder sb = new StringBuilder();
			modules.forEach((k,v)->{
				System.out.println("process module " + k + " ......");
				v.containerMap.forEach((ck,cv)->{
					sb.append("<object name=\"or_").append(ck).append("\" extends=\"CommonXMLBean\" objectType=\"xmlBean\" __name=\"").
					append(ck).append("\" auto-create=\"yes\">");
					sb.append("<metaInfo><metaItem name=\"__uri\" value=\"").append(v.namespace).append("\"/></metaInfo>");
					cv.toXML(modules,v,sb);
					sb.append("</object>");
				});
			});
			System.out.println("\n\n"+sb.toString());
		}
	}

	public int fromYang (Element node) {
		Yang yang = new Yang();
		yang.parse(node);
		yang.toXML();
		return 0;
	}

	public static void main(String argv[]) {
		if ( argv.length != 1 )
		{
			System.err.println("please input document url");
			System.exit( 1 );
		}
		String file = argv[0];
		YangFileUtil util = new YangFileUtil();
		try
		{
			String newFile = file+".new";
			SAXReader reader = new SAXReader();
			Document doc = reader.read(file);
			Element root = doc.getRootElement();
			//          int changedNum = util.addAutoCreate(root);
			// int changedNum = util.addSuperRelation(root);    
			int changedNum = util.fromYang(root);
			if (changedNum > 0) {
				System.out.println("changed number:"+changedNum);
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setIndentSize(2);
				FileWriter fileWriter = new FileWriter(newFile);
				XMLWriter writer = new XMLWriter(fileWriter, format);
				writer.write(doc);
				writer.close();
			} else {
				System.out.println("No Change");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
