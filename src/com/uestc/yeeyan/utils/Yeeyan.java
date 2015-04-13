package com.uestc.yeeyan.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
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
	private final String logFilePath = System.getProperty("user.dir")
			+ File.separator + "YeeyanLog.log";

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
				;
			} catch (IOException e) {
				;
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
		// System.out.println("Max page count:" + maxPageCount);
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
			try {
				String regex = "(?<=<a target=\"_blank\" href=\")http://select.yeeyan.org/view/\\d+/\\d+(?=\">)";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(htmlCode);
				while (matcher.find()) {
					String link = matcher.group();
					if (!linkLists.contains(link)) {
						linkLists.offer(link);
					}
				}
			} catch (Exception e) {
				;
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
			parser.reset();// 重置parser内容 否则parser内容为上次match之后的节点
			NodeList nodeList = parser.extractAllNodesThatMatch(filter);
			content = nodeList.elementAt(0).toPlainTextString();
			return content;
		} catch (ParserException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.err
					.println("Attribute not found!You may have to modify the attribute String[].");
		}
		return "";
	}

	public String getMD5(String inputText) {
		try {
			byte[] btInputs;
			btInputs = inputText.getBytes("utf-8");
			MessageDigest mdInstance = MessageDigest.getInstance("MD5");
			mdInstance.update(btInputs);
			byte[] md = mdInstance.digest();
			String md5 = new BigInteger(1, md).toString(16);
			return md5;
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getContentHtml(Parser parser) {
		parser.reset();
		NodeList list;
		String html = null;
		try {
			list = parser
					.extractAllNodesThatMatch(getAttributeFilter(new String[] {
							"class", "sa_content" }));
			html = list.elementAt(0).toHtml();

		} catch (ParserException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return html;
	}

	private class ImageDownloader implements Runnable {
		private String imageUrl, storeDir, imageRelativePath;

		public ImageDownloader(String imageUrl, String storeDir,
				String imageRelativePath) {
			this.imageUrl = imageUrl;
			this.storeDir = storeDir;
			this.imageRelativePath = imageRelativePath;
		}

		public void run() {
			File dir = new File(storeDir);
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}
			OutputStream os = null;
			InputStream is = null;
			try {
				URL url = new URL(imageUrl);
				File outFile = new File(storeDir + File.separator
						+ imageRelativePath);
				os = new FileOutputStream(outFile);
				is = url.openStream();
				byte[] buffer = new byte[1024];
				int length = -1;
				while ((length = is.read(buffer)) != -1) {
					os.write(buffer, 0, length);
				}
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
					if (os != null) {
						os.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean downloadInTimeout(ImageDownloader downloader, int timeout) {
		Thread t = new Thread(downloader);
		t.start();
		try {
			t.join(timeout);
			return true;
		} catch (InterruptedException e) {
			System.err.println("Iamge download timeout in "
					+ downloader.imageUrl);
			return false;
		}
	}

	public void storeContent(String targetUrl) {
		try {
			String htmlCode = getHtmlCode(targetUrl);
			Parser parser = Parser.createParser(htmlCode, this.encoding);
			String _abstract = getFiltContent(parser, new String[] { "class",
					"sa_abstract" });
			// System.out.println("_abstract:" + _abstract);
			String title = getFiltContent(parser, new String[] { "class",
					"sa_title sa_title_pr" });
			// System.out.println("title:" + title);
			String content = getFiltContent(parser,
					new String[] { "class", "sa_content" }).replaceAll(
					"\\n\\s*", "\n").replaceAll("&nbsp;", " ");
			// System.out.println("Content:" + content);
			// 用正则表达式获取时间 当通过sa_author获取不准确时可用
			// String timeReg =
			// "(?<=<span>发布：)\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?=</span>)";
			// Pattern timePattern = Pattern.compile(timeReg);
			// Matcher timeMatcher = timePattern.matcher(htmlCode);
			// String date = null;
			// if(timeMatcher.find()){
			// date = timeMatcher.group();
			// System.out.println(date);
			// }
			// else{
			// System.err.println("Date not found!");
			// }
			String rawDate = getFiltContent(parser, new String[] { "class",
					"sa_author" });
			Date date = null;
			try {
				String sdate = rawDate.substring(
						rawDate.indexOf("发布：") + "发布：".length(),
						rawDate.lastIndexOf("挑错"));
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				date = sdf.parse(sdate);
			} catch (StringIndexOutOfBoundsException e) {
				date = null;
			}
			String MD5 = getMD5(content);

			Date time = new Date();
			// <img
			// src="http://static.yeeyan.org/upload/image/2015/03/13/14262218586.jpg"
			// />
			String imgReg = "((?<=src=\")http://static.yeeyan.org/upload/image/\\d{4}/\\d{2}/\\d{2}/\\d+\\.((jpg)|(png)|(bmp)|(jpeg)|(gif))(?=\"))|((?<=src=\")http://cdn.yeeyan.org/upload/image/\\d{4}/\\d{2}/(.*?)\\.((jpg)|(png)|(bmp)|(jpeg)|(gif))(?=\"))";
			Pattern pattern = Pattern.compile(imgReg);

			String contentHtml = getContentHtml(parser);
			Matcher matcher = pattern.matcher(contentHtml);
			int imageNum = 1;
			Queue<String> imageUrlQueue = new LinkedList<String>();
			Queue<String> downloadedImageQueue = new LinkedList<String>();
			while (matcher.find()) {
				String imageUrl = matcher.group();
				if (imageUrlQueue.contains(imageUrl)) {
					continue;
				} else {
					imageUrlQueue.offer(imageUrl);
				}
				String postfix = imageUrl.substring(
						imageUrl.lastIndexOf(".") + 1, imageUrl.length())
						.replace(" ", "");
				String relativePath = String.format("%s_%04d.%s", MD5,
						imageNum, postfix);
				ImageDownloader downloader = new ImageDownloader(imageUrl,
						this.storeDir, relativePath);
				int timeout = 60 * 1000; // 1 min
				String imagePath = String.format("%s%s%s", this.storeDir,
						File.separator, relativePath);
				if (downloadInTimeout(downloader, timeout)) {
					downloadedImageQueue.offer(imagePath);
					imageNum++;
				}
			}
			
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			DB db = mongoClient.getDB("Yeeyan");
			DBCollection coll = db.getCollection("yeeyan");
			BasicDBObject doc = new BasicDBObject("Abstract", _abstract)
					.append("Title", title).append("Content", content)
					.append("Date", date)
					.append("image", downloadedImageQueue.toString())
					.append("Url", targetUrl).append("MD5", MD5)
					.append("Time", time).append("NewSource", "译言网");
			BasicDBObject query = new BasicDBObject("Url", targetUrl);
			DBCursor cursor = coll.find(query);
			// 如果记录中没有相同Url的文章则添加记录
			if (!cursor.hasNext()) {
				coll.insert(doc);
			} else {
				// System.out.println("Find same record");
			}
			mongoClient.close();
		} catch (Exception e) {
			String msg = "";
			try {
				File file = new File(logFilePath);
				try {
					FileWriter f = new FileWriter(file, true);
					f.write(String.format("Url: %s\t%s\n", targetUrl,
							e.getMessage()));
					f.close();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (Exception exc) {
				msg = exc.getMessage();
			} finally {
				System.out.println(targetUrl + "===>" + msg);
			}
		}

	}

	public void refreshTo(int targetPageNum) {
		System.out.println("Refreshing to first "
				+ Integer.valueOf(targetPageNum) + " page(s)...");
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

	public static class YeeyanTimer {

		public void schedule() {
			Timer timer = new Timer();
			int timeOut = 12 * 60 * 60 * 1000; // half an hour
			timer.schedule(new YeeyanTask(), 0, timeOut);
		}

		class YeeyanTask extends TimerTask {
			private final int refreshPageNum = 5;

			@Override
			public void run() {
				System.out.println(String.format(
						"Refreshing first %d page(s)...", refreshPageNum));
				new Yeeyan().refreshTo(refreshPageNum);
				return;
			}
		}
	}

	public static void main(String[] args) {

		Yeeyan yeeyan = new Yeeyan();
		yeeyan.CrawlALL();

		YeeyanTimer timer = new YeeyanTimer();
		timer.schedule();
		// Date date = new Date(Calendar.getInstance().getTimeInMillis());
		// System.out.println(date);
		// yeeyan.storeContent("http://article.yeeyan.org/view/194087/174815");

	}

}