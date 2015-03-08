package ch.ethz.twimight.net.Html;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("NewApi")

public abstract class WebArchiveReader {
	private static final String TAG = "Web Reader";
    private Document myDoc = null;
    private static boolean myLoadingArchive = false;
    private WebView myWebView = null;
    private ArrayList<String> urlList = new ArrayList<String>();
    private ArrayList<Element> urlNodes = new ArrayList<Element>();

    protected abstract void onFinished(WebView webView);

    public boolean readWebArchive(InputStream is) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        myDoc = null;
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            myDoc = builder.parse(is);
            NodeList nl = myDoc.getElementsByTagName("url");
            for (int i = 0; i < nl.getLength(); i++) {
                Node nd = nl.item(i);
                if(nd instanceof Element) {
                    Element el = (Element) nd;
                    // siblings of el (url) are: mimeType, textEncoding, frameName, data
                    NodeList nodes = el.getChildNodes();
                    for (int j = 0; j < nodes.getLength(); j++) {
                        Node node = nodes.item(j);
                        if (node instanceof Text) {
                            String dt = ((Text)node).getData();
                            byte[] b = Base64.decode(dt, Base64.DEFAULT);
                            dt = new String(b);
                            urlList.add(dt);
                            urlNodes.add((Element) el.getParentNode());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            myDoc = null;
        }
        return myDoc != null;
    }

    private byte [] getElBytes(Element el, String childName) {
        try {
            Node kid = el.getFirstChild();
            while (kid != null) {
                if (childName.equals(kid.getNodeName())) {
                    Node nn = kid.getFirstChild();
                    if (nn instanceof Text) {
                        String dt = ((Text)nn).getData();
                        return Base64.decode(dt, Base64.DEFAULT);
                    }
                }
                kid = kid.getNextSibling();
            }
        } catch (Exception e) {
        	Log.e(TAG,"error",e);
        }
        return null;
    }

    public boolean loadToWebView(WebView v) throws Exception {
        myWebView = v;
        v.setWebViewClient(new WebClient());
        
        myLoadingArchive = true;       
            // Find the first ArchiveResource in myDoc, should be <ArchiveResource>
            Element ar = (Element) myDoc.getDocumentElement().getFirstChild().getFirstChild();
            byte b[] = getElBytes(ar, "data");
            String topHtml = new String(b);
            String baseUrl = new String(getElBytes(ar, "url"));
            v.loadDataWithBaseURL(baseUrl, topHtml, "text/html", "UTF-8", null);
        
        return true;
    }

    private class WebClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (!myLoadingArchive)
                return null;
            int n = urlList.indexOf(url);
            if (n < 0)
                return null;
            Element parentEl = urlNodes.get(n);
            byte [] b = getElBytes(parentEl, "mimeType");
            String mimeType = b == null ? "text/html" : new String(b);
            b = getElBytes(parentEl, "textEncoding");
            String encoding = b == null ? "UTF-8" : new String(b);
            b = getElBytes(parentEl, "data");
            return new WebResourceResponse(mimeType, encoding, new ByteArrayInputStream(b));
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            // our WebClient is no longer needed in view
            view.setWebViewClient(null);
            myLoadingArchive = false;
            onFinished(myWebView);
        }
    }
}