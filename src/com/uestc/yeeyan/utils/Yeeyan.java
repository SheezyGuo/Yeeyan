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

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
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

	public Queue<String> getLinkLists() {
		String urlHead = "http://select.yeeyan.org/lists/all/horizontal/";
		Queue<String> linkLists = new LinkedList<String>();
		for (int i = 1; i <= this.maxPageCount; i++) {
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
		return null;
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
			@SuppressWarnings("resource")
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
		String content = getFiltContent(parser, new String[] { "class","sa_content" });
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
		String MD5 = getMD5(htmlCode);
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
			String relativePath = String.format("%s%04d.%s", MD5, imageNum,
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
		coll.insert(doc);
		mongoClient.close();

	}

	public void refreshTo(int targetPageNum) {
		Queue<String> linkLists = this.getLinkLists();
		int now = 1;
		for (String link = null; now >= targetPageNum; now++) {
			System.out.println(String.format("%d/%d proceeding...", now,
					targetPageNum));
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
		//yeeyan.storeContent("http://select.yeeyan.org/view/305107/446753");
		String[] strs = new String[]{
				"���㷨������MD5��ȫ�Ը��ߣ�DES����̭����ͨ����",
				"Ϣ���ܣ�������Ϥ�ļ�ʱͨ�����MyIM��������ǩ������෽�档",
				"MD5�����ⳤ�ȵġ��ֽڴ����任��һ��128bit�Ĵ���������������һ�����������,",
				"�����任�㷨�����仰˵���ǣ���ʹ�㿴��Դ������㷨������Ҳ�޷���һ��MD5��ֵ�任��ԭʼ���ַ���������ѧԭ����˵������Ϊԭʼ���ַ����������������е��󲻴��ڷ���������ѧ������",
				"MD5�ĵ���Ӧ���Ƕ�һ��Message(�ֽڴ�)����fingerprint(ָ��)���Է�ֹ�����۸ġ����ٸ����ӣ��㽫һ�λ�д��һ���� readme.txt�ļ��У�",
				"�������readme.txt����һ��MD5��ֵ����¼�ڰ���Ȼ������Դ�������ļ�����"
		};
		for(String str:strs){
			System.out.println(yeeyan.getMD5(str));
		}
	}
}