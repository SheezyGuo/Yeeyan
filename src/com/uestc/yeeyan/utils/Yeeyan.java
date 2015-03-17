package com.uestc.yeeyan.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class Yeeyan {

	private String mainPageUrl = "http://select.yeeyan.org/lists/all/horizontal/1";
	private int maxPageCount = -1;
	private String encoding = "utf-8";
	private final String storeDir = System.getProperty("user.dir")
			+ File.separator + "YeeyanImages";

	public Yeeyan() {
		this.setMaxPageCount();
	}

	public String getHtmlCode(String targetUrl) {
		String htmlCode = "";
		int tryCount = 5;
		while (tryCount > 0) {
			try {
				URL url = new URL(targetUrl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setReadTimeout(5000);
				conn.setRequestProperty("User-Agent",
						"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
				conn.connect();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), encoding));
				String line = "";
				while ((line = reader.readLine()) != null) {
					htmlCode += line + "\n";
				}
				tryCount = 0;
				return htmlCode;
			} catch (MalformedURLException e) {
				System.out.println("无法建立Url连接:" + targetUrl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				tryCount--;
			}
		}
		return null;
	}

	private void setMaxPageCount() {
		String htmlCode = getHtmlCode(mainPageUrl);
		String desPart = htmlCode
				.substring(
						htmlCode.indexOf("<li>...</li>")
								+ "<li>...</li>".length(),
						htmlCode.indexOf("<li><a  href=\"/lists/all/horizontal/2\" >下一页</a></li>"));
		String strInt = desPart.substring(
				desPart.indexOf("href=\"/lists/all/horizontal/")
						+ "href=\"/lists/all/horizontal/".length(),
				desPart.indexOf("\" >"));
		this.maxPageCount = Integer.valueOf(strInt);
		System.out.println("Max page count:" + maxPageCount);
	}

	public Queue<String> getLinkLists(int targetPageNum) {
		String urlHead = "http://select.yeeyan.org/lists/all/horizontal/";
		Queue<String> linkLists = new LinkedList<String>();
		for (int i = 1; i <= targetPageNum; i++) {
			String htmlCode = this.getHtmlCode(urlHead + String.valueOf(i));
			if (htmlCode == null) {
				System.err.println(urlHead + String.valueOf(i) + "===>未能正确打开！");
				continue;
			}
			String regex = "(?<=<a target=\"_blank\" href=\")http://select.yeeyan.org/view/\\d+/\\d+(?=\">)";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(htmlCode);
			while (matcher.find()) {
				String link = matcher.group();
				linkLists.offer(link);
			}
		}
		return linkLists;
	}

	public NodeFilter getAttributeFilter(String[] attrs) {
		if (attrs.length == 1) {
			return new HasAttributeFilter(attrs[0]);
		} else if (attrs.length == 2) {
			return new HasAttributeFilter(attrs[0], attrs[1]);
		} else {
			System.err.println("Usage:getAttributeFilter(String[1|2] attrs)");
			return null;
		}
	}

	public String getFiltContent(Parser parser, String[] attrs) {
		String content = "";
		NodeFilter filter = getAttributeFilter(attrs);
		try {
			parser.reset();//重置parser内容 否则parser内容为上次match之后的节点
			NodeList nodeList = parser.extractAllNodesThatMatch(filter);
			content = nodeList.elementAt(0).toPlainTextString();
			return content;
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.err.println("Attribute not found!You may have to modify the attribute String[].");
		}
		return "Not found";
	}

	public String getMD5(String inputText) {
		try {
			byte[] btInputs;
			btInputs = inputText.getBytes("utf-8");
			MessageDigest mdInstance = MessageDigest.getInstance("MD5");
			mdInstance.update(btInputs);
			byte[] md = mdInstance.digest();
			String md5 = new BigInteger(1,md).toString(16);
			return md5;
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void downloadImage(String imageUrl, String storeDir,
			String imageRelativePath) {
		File dir = new File(storeDir);
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		try {
			URL url = new URL(imageUrl);
			File outFile = new File(storeDir + File.separator
					+ imageRelativePath);
			OutputStream os = new FileOutputStream(outFile);
			InputStream is = url.openStream();
			byte[] buffer = new byte[10240];
			int length = -1;
			while ((length = is.read(buffer)) != -1) {
				os.write(buffer, 0, length);
			}
			is.close();
			os.close();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void storeContent(String targetUrl) {
		String htmlCode = getHtmlCode(targetUrl);
		Parser parser = Parser.createParser(htmlCode, this.encoding);
		String _abstract = getFiltContent(parser, new String[] { "class",
				"sa_abstract" });
		System.out.println("_abstract:"+_abstract);
		String title = getFiltContent(parser, new String[] { "class",
				"sa_title sa_title_pr" });
		System.out.println("title:"+title);
		String content = getFiltContent(parser, new String[] { "class","sa_content" }).replaceAll("\\n\\s*","\n").replaceAll("&nbsp;","");
		System.out.println("Content:"+content);
// 用正则表达式获取时间 当通过sa_author获取不准确时可用		
//		String timeReg = "(?<=<span>发布：)\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?=</span>)";
//		Pattern timePattern = Pattern.compile(timeReg);
//		Matcher timeMatcher = timePattern.matcher(htmlCode);
//		String date = null;
//		if(timeMatcher.find()){ 
//			date = timeMatcher.group();
//			System.out.println(date);
//		}
//		else{
//			System.err.println("Date not found!");
//		}
		String rawDate = getFiltContent(parser, new String[] { "class","sa_author" });
		String date = rawDate.substring(rawDate.indexOf("发布：")+"发布：".length(),rawDate.lastIndexOf("挑错"));
		String MD5 = getMD5(content);
		// <img
		// src="http://static.yeeyan.org/upload/image/2015/03/13/14262218586.jpg"
		// />
		String imgReg = "(?<=<img src=\")http://static.yeeyan.org/upload/image/\\d{4}/\\d{2}/\\d{2}/\\d+\\.((jpg)|(png)|(bmp)|(jpeg))(?=\" />)";
		Pattern pattern = Pattern.compile(imgReg);
		Matcher matcher = pattern.matcher(htmlCode);
		int imageNum = 1;
		Queue<String> imagePathQueue = new LinkedList<String>();
		while (matcher.find()) {
			String imageUrl = matcher.group();
			System.out.println("imageUrl:"+imageUrl);
			String postfix = imageUrl.substring(imageUrl.lastIndexOf(".")+1,
					imageUrl.length()).replace(" ", "");
			String relativePath = String.format("%s_%04d.%s", MD5, imageNum,
					postfix);
			downloadImage(imageUrl, this.storeDir, relativePath);
			String imagePath = String.format("%s%s%s", storeDir,
					File.separator, relativePath);
			imagePathQueue.offer(imagePath);
			imageNum++;
		}
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		DB db = mongoClient.getDB("Yeeyan");
		DBCollection coll = db.getCollection("yeeyan");
		BasicDBObject doc = new BasicDBObject("abstract",_abstract).append("Title", title)
				.append("Content", content).append("Date", date)
				.append("ImagePath", imagePathQueue.toString())
				.append("URL", targetUrl).append("MD5", MD5);
		BasicDBObject query = new BasicDBObject("MD5",MD5);
		DBCursor cursor = coll.find(query); 
		//如果记录中没用相同MD5值的文章则添加记录
		if(!cursor.hasNext()){
			coll.insert(doc);
		}
		else{
			//System.out.println("Find same record");
		}
		mongoClient.close();

	}

	public void refreshTo(int targetPageNum) {
		Queue<String> linkLists = this.getLinkLists(targetPageNum);
		int pageNum = linkLists.size();
		int now = 1;
		for (String link = null; now <= pageNum; now++) {
			System.out.println(String.format("%d/%d proceeding...", now,
					pageNum));
			link = linkLists.poll();
			storeContent(link);
		}
	}

	public void CrawlALL() {
		this.refreshTo(this.maxPageCount);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Yeeyan yeeyan = new Yeeyan();
		yeeyan.CrawlALL();
//		yeeyan.refreshTo(30);
		
//		String[] strs = new String[]{
//				"\n\t\t\t\t\n\t作者：Gerd\nLeonhard \n\n\n\t \n\n超级互联的未来既美好又可怕，正如新加坡的这些高科技“超级树”。\n\n\t智能手机和移动设备给了所有人第二个大脑，为身处陌生城市中的我们进行导航，不致迷路，同时，帮助我们用多种语言进行顺畅的交流。\n\n\n\t这些设备使得我们的生活越来越便利，很快就将从衣服口袋转移至体内——试想一下虹膜植入和大脑植入，赋予我们前所未有的互联性。\n\n\n\t显然，移动互联的乐趣和便利不胜枚举，令人无法抗拒。若想置之不理，无疑是徒劳之举。\n\n\n\t不过在不久的将来，我料想一系列意料之外的扰人后果将会显现，超级互联的美好未来将渐渐化为泡影。\n\n\n\t我们在Flickr上分享自己孩子照片的时候，也许不会想到它们会出现在Koppie-Koppie网站正在出售的咖啡杯上。\n\n\n\t在推特上评论政治应该不至于让人身陷囹圄，不过这样的事目前正在土耳其上演。\n\n\n\t为脸书的某家公司点赞不应沦为人人都能参与的市场营销。\n\n\n\t拥有一部带有SIM卡的手机不应沦为政府安全部门监听公众电话的永恒捷径。\n\n\n\t这类事件层出不穷，以上只是冰山一角。互联技术的滥用趋势如火如荼。\n\n\n\t2020年，预计有超过50亿人、1000亿“事物”将连接至互联网——其中相当一部分通过无线、移动设备等手段进行连接。\n\n\n\t人工智能等科技的飞速发展和无处不在的互联技术正逐步向核能靠拢。核能技术前景辽阔，颇有益处，不过实际应用后，一旦出现什么差池，我们压根不清楚如何加以制止。 \n\n\n\t愈加显而易见的一点是，若没有强制性的全球标准明确什么该允许，什么又该禁止，日后我们将频繁遭遇灾难，或者沦为另一个福岛[1]。\n\n\n\t若没有相应法规的出台，灾难性事件一触即发。我们需要制定标准和规则来疏导这股强大的力量。\n\n\n\t消费者需要强大的科技，让生活更便利、更丰富、更高效。不过，假如科技意味着整个社会逐渐向“机器人化”的方向发展，意味着堕落与沉迷，意味着个人隐私的丧失和无端的监控，那么他们是不会使用这些移动设备和网络的。\n\n\n\t确实，我们中的大多数人目前仍在享受全新移动技术，超级便利的手机软件、地图以及智能数字助手所具备的“超能力”。这些技术和设备如此新鲜、富有个性，以致很多人根本不在乎自己在虚拟世界变得毫无隐私可言。\n\n\n\t不过可以肯定的是，我们中的许多“数字原住民”很快将奋起反抗，拒绝再居住在这样一个庞大的“大数据”泡沫里。身处其中，我们不再是科技的主人或者说受益人，只会沦为无足轻重的人，沦为过度营销和远程监控的受害者。\n\n\n\t整个游戏已经发生变化，移动行业最好注意到这一点。建议行业领导者们目光放长远一些。\n\n\n\t问题的关键不再是如何实现人与人的互联，而是一旦实现这一目标，将会发生什么。\n\n\n\t如果疲软的数据保护标准、过时的社会契约以及摇摇欲坠的道德底线成为一种社会风气，那么极少有消费者会对数字货币、可穿戴设备和移动健康服务激动不已。\n\n\n\t是时候将加密技术视为一种默认的公共电子数据标准和人人都尊重的普遍权利了。\n\n\n\t\n\n\n\t译注：\n\n\n\t[1]福岛第一核电站在2011年“3·11”大地震和海啸中遭受重创，引发严重的辐射泄漏事故。\n\n\n\t\n\n\n\t\n\t\t————————————————————————————————————————\n\t\n\t\n\t\t*未经本人许可，请勿转载！\n\t\n\n\n\t———————————————————————————————————————— \n\n\n\t* 本译文来自译言「近未来」小组，是译言的科技主题翻译项目,欢迎加入。组内的优秀译文（以编译为主）将被收录于腾讯出品的思想内刊《腾云》（采用有稿酬）。译文未经授权请勿转载，合作及授权请联系：editor@yeeyan.com \n\n\n\t\t\t"				
//		};
//		for(String str:strs){
//			System.out.println(yeeyan.getMD5(str));
//		}
//		String code = yeeyan.getHtmlCode("http://select.yeeyan.org/view/305107/446753");
//		Parser parser = Parser.createParser(code, "utf-8");
//		String content = yeeyan.getFiltContent(parser, new String[]{"class","sa_content"});
//		System.out.println(content.replaceAll("\\n\\s*","\n"));
	}
}
//嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿嘿
