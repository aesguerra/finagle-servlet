package it.unipi.di.acube.finagle.servlet;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.finagle.Service;
import com.twitter.finagle.http.Method;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.http.Version;
import com.twitter.util.Await;
import com.twitter.util.Future;

import scala.Tuple2;
import scala.collection.JavaConverters;

public abstract class FinagleServletAdapter {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void serve(Service<Request, Response> finagleService, HttpServletRequest servletRequest,
	        HttpServletResponse servletResponse) {
		try {
			Request request = toFinagleRequest(servletRequest);
			LOG.debug("Forwarding request to Finagle Service. method=%s uri=%s content_length=%d content=%s", request.method(),
			        request.uri(), request.getLength(), request.getContentString());
			Future<Response> resp = finagleService.apply(request);
			Response c = Await.result(resp);
			toServletResponse(c, servletResponse);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e, servletResponse);
		}
	}

	public static void toServletResponse(Response finagleResponse, HttpServletResponse servletResponse) throws IOException {
		servletResponse.setStatus(finagleResponse.statusCode());

		Collection<Tuple2<String, String>> finagleHeaders = JavaConverters.asJavaCollectionConverter(finagleResponse.headerMap().toList()).asJavaCollection();
		for (Tuple2<String, String> headerKV : finagleHeaders)
			servletResponse.setHeader(headerKV._1, headerKV._2);

		byte[] arr = finagleResponse.content().copiedByteArray();

		servletResponse.getOutputStream().write(arr);
		servletResponse.setContentLength(arr.length);

		LOG.debug("Call to Finagle succeeded. Forwarding reply of length %d", arr.length);
	}

	public static Request toFinagleRequest(HttpServletRequest httpServletRequest) throws IOException {
		String watUri = httpServletRequest.getRequestURI().substring(httpServletRequest.getContextPath().length());

		// Add URL parameters, if any
		if (httpServletRequest.getQueryString() != null)
			watUri += "?" + httpServletRequest.getQueryString();

		Version version = httpServletRequest.getProtocol().equals("HTTP/1.0") ? Version.Http10() : Version.Http11();
		Method method = Method.apply(httpServletRequest.getMethod());

		Request r = Request.apply(version, method, watUri);

		// Copy headers
		for (String headerName : Collections.list(httpServletRequest.getHeaderNames())) {
			String headerValue = httpServletRequest.getHeader(headerName);
			r.headerMap().set(headerName, headerValue);
		}

		// Copy response content
		r.setContentString(IOUtils.toString(httpServletRequest.getInputStream(), ("UTF-8")));

		return r;
	}

	private static void fail(Throwable e, HttpServletResponse servletResponse) {
		servletResponse.setStatus(500);
		try {
			e.printStackTrace(new PrintStream(servletResponse.getOutputStream()));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
