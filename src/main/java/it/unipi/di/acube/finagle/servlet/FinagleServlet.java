package it.unipi.di.acube.finagle.servlet;

import java.lang.invoke.MethodHandles;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.finagle.Service;
import com.twitter.finagle.ServiceFactory;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;

public class FinagleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private Service<Request, Response> finagleService = null;
	private ServiceFactory<Request, Response> finagleServiceFactory;

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) {
		FinagleServletAdapter.serve(finagleService, req, resp);
	}

	@Override
	public void init(ServletConfig config) {
		String className = config.getInitParameter("service.factory.classname");

		LOG.info("Starting Finagle Servlet with class: " + className);

		try {
			FinagleServiceFactoryProvider factory = (FinagleServiceFactoryProvider) Class.forName(className).newInstance();
			finagleServiceFactory = factory.provide(config);
			finagleService = finagleServiceFactory.toService();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
		LOG.info("Shutting down Finagle Servlet.");
		finagleServiceFactory.close();
	}
}
