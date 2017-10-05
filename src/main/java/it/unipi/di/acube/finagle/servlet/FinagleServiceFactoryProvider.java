package it.unipi.di.acube.finagle.servlet;

import javax.servlet.ServletConfig;

import com.twitter.finagle.ServiceFactory;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;

public interface FinagleServiceFactoryProvider {
	public ServiceFactory<Request, Response> provide(ServletConfig config);
}
