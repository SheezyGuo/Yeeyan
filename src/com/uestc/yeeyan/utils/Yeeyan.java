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
				System.out.println("�޷�����Url����:" + targetUrl);
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
						htmlCode.indexOf("<li><a  href=\"/lists/all/horizontal/2\" >��һҳ</a></li>"));
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
				System.err.println(urlHead + String.valueOf(i) + "===>δ����ȷ�򿪣�");
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
			parser.reset();//����parser���� ����parser����Ϊ�ϴ�match֮��Ľڵ�
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
// ��������ʽ��ȡʱ�� ��ͨ��sa_author��ȡ��׼ȷʱ����		
//		String timeReg = "(?<=<span>������)\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?=</span>)";
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
		String date = rawDate.substring(rawDate.indexOf("������")+"������".length(),rawDate.lastIndexOf("����"));
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
		//�����¼��û����ͬMD5ֵ����������Ӽ�¼
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
//				"\n\t\t\t\t\n\t���ߣ�Gerd\nLeonhard \n\n\n\t \n\n����������δ���������ֿ��£������¼��µ���Щ�߿Ƽ�������������\n\n\t�����ֻ����ƶ��豸���������˵ڶ������ԣ�Ϊ��İ�������е����ǽ��е�����������·��ͬʱ�����������ö������Խ���˳���Ľ�����\n\n\n\t��Щ�豸ʹ�����ǵ�����Խ��Խ�������ܿ�ͽ����·��ڴ�ת�������ڡ�������һ�º�Ĥֲ��ʹ���ֲ�룬��������ǰ��δ�еĻ����ԡ�\n\n\n\t��Ȼ���ƶ���������Ȥ�ͱ�����ʤö�٣������޷����ܡ�������֮����������ͽ��֮�١�\n\n\n\t�����ڲ��õĽ�����������һϵ������֮������˺���������֣���������������δ����������Ϊ��Ӱ��\n\n\n\t������Flickr�Ϸ����Լ�������Ƭ��ʱ��Ҳ�����뵽���ǻ������Koppie-Koppie��վ���ڳ��۵Ŀ��ȱ��ϡ�\n\n\n\t����������������Ӧ�ò�������������������������������Ŀǰ�������������ݡ�\n\n\n\tΪ�����ĳ�ҹ�˾���޲�Ӧ��Ϊ���˶��ܲ�����г�Ӫ����\n\n\n\tӵ��һ������SIM�����ֻ���Ӧ��Ϊ������ȫ���ż������ڵ绰������ݾ���\n\n\n\t�����¼�����������ֻ�Ǳ�ɽһ�ǡ������������������������ݱ��\n\n\n\t2020�꣬Ԥ���г���50���ˡ�1000�ڡ���������������������������൱һ����ͨ�����ߡ��ƶ��豸���ֶν������ӡ�\n\n\n\t�˹����ܵȿƼ��ķ��ٷ�չ���޴����ڵĻ���������������ܿ�£�����ܼ���ǰ�������������洦������ʵ��Ӧ�ú�һ������ʲô��أ�����ѹ���������μ�����ֹ�� \n\n\n\t�����Զ��׼���һ���ǣ���û��ǿ���Ե�ȫ���׼��ȷʲô������ʲô�ָý�ֹ���պ����ǽ�Ƶ���������ѣ�������Ϊ��һ������[1]��\n\n\n\t��û����Ӧ����ĳ�̨���������¼�һ��������������Ҫ�ƶ���׼�͹������赼���ǿ���������\n\n\n\t��������Ҫǿ��ĿƼ�������������������ḻ������Ч������������Ƽ���ζ������������򡰻����˻����ķ���չ����ζ�Ŷ�������ԣ���ζ�Ÿ�����˽��ɥʧ���޶˵ļ�أ���ô�����ǲ���ʹ����Щ�ƶ��豸������ġ�\n\n\n\tȷʵ�������еĴ������Ŀǰ��������ȫ���ƶ������������������ֻ��������ͼ�Լ����������������߱��ġ�������������Щ�������豸������ʡ����и��ԣ����ºܶ��˸������ں��Լ������������ú�����˽���ԡ�\n\n\n\t�������Կ϶����ǣ������е���ࡰ����ԭס�񡱺ܿ콫���𷴿����ܾ��پ�ס������һ���Ӵ�ġ������ݡ���ĭ������У����ǲ����ǿƼ������˻���˵�����ˣ�ֻ����Ϊ�������ص��ˣ���Ϊ����Ӫ����Զ�̼�ص��ܺ��ߡ�\n\n\n\t������Ϸ�Ѿ������仯���ƶ���ҵ���ע�⵽��һ�㡣������ҵ�쵼����Ŀ��ų�ԶһЩ��\n\n\n\t����Ĺؼ����������ʵ�������˵Ļ���������һ��ʵ����һĿ�꣬���ᷢ��ʲô��\n\n\n\t���ƣ������ݱ�����׼����ʱ�������Լ�Լ�ҡҡ��׹�ĵ��µ��߳�Ϊһ������������ô�����������߻�����ֻ��ҡ��ɴ����豸���ƶ��������񼤶����ѡ�\n\n\n\t��ʱ�򽫼��ܼ�����Ϊһ��Ĭ�ϵĹ����������ݱ�׼�����˶����ص��ձ�Ȩ���ˡ�\n\n\n\t\n\n\n\t��ע��\n\n\n\t[1]������һ�˵�վ��2011�ꡰ3��11�������ͺ�Х�������ش����������صķ���й©�¹ʡ�\n\n\n\t\n\n\n\t\n\t\t��������������������������������������������������������������������������������\n\t\n\t\n\t\t*δ��������ɣ�����ת�أ�\n\t\n\n\n\t�������������������������������������������������������������������������������� \n\n\n\t* �������������ԡ���δ����С�飬�����ԵĿƼ����ⷭ����Ŀ,��ӭ���롣���ڵ��������ģ��Ա���Ϊ����������¼����Ѷ��Ʒ��˼���ڿ������ơ��������и�꣩������δ����Ȩ����ת�أ���������Ȩ����ϵ��editor@yeeyan.com \n\n\n\t\t\t"				
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
//�ٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺٺ�
