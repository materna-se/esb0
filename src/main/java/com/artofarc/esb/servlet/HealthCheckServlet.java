package com.artofarc.esb.servlet;

import com.artofarc.esb.Registry;
import com.artofarc.esb.context.WorkerPool;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@WebServlet(urlPatterns = {"/healthz", "/readyz"})
public class HealthCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String CONTENT_TYPE = "text/plain";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Registry registry = (Registry) getServletContext().getAttribute(ESBServletContextListener.CONTEXT);

		String path = request.getRequestURI();
		if (path.endsWith("/healthz")) {
			handleLivenessProbe(registry, response);
		}
		else if (path.endsWith("/readyz")) {
			handleReadinessProbe(registry, response);
		}
		else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * Handles the Liveness Probe.
	 * It calls a dedicated checker method to determine the application's liveness.
	 */
	private void handleLivenessProbe(Registry registry, HttpServletResponse response) throws IOException {
		String livenessError = checkApplicationLiveness(registry);
		if (livenessError == null) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(CONTENT_TYPE);
			response.getWriter().write("ALIVE");
		}
		else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(CONTENT_TYPE);
			response.getWriter().write("DEAD: " + livenessError);
		}
	}

	/**
	 * Handles the Readiness Probe.
	 * It calls a dedicated checker method to determine the application's readiness.
	 */
	private void handleReadinessProbe(Registry registry, HttpServletResponse response) throws IOException {
		String readinessError = checkReadiness(registry);
		if (readinessError == null) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(CONTENT_TYPE);
			response.getWriter().write("READY");
		}
		else {
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			response.setContentType(CONTENT_TYPE);
			response.getWriter().write("NOT READY: " + readinessError);
		}
	}

	/**
	 * Performs a check of the application's liveness.
	 *
	 * @param registry The application registry.
	 * @return null if the application is live, otherwise a string describing the reason.
	 */
	private String checkApplicationLiveness(Registry registry) {
		// 1. registry must exist, this means the servlet context has been initialized
		if (registry == null) {
			return "registry not initialized";
		}

		return null;
	}

	/**
	 * Performs a check of the application's readiness.
	 *
	 * @param registry The application registry.
	 * @return null if the application is ready, otherwise a string describing the reason.
	 */
	private String checkReadiness(Registry registry) {
		// 1. registry must exist, this means the servlet context has been initialized
		if (registry == null) {
			return "registry not initialized";
		}

		// 2. there must be at least one worker pool configured
		Collection<WorkerPool> workerPools = registry.getWorkerPools();
		if (workerPools.isEmpty()) {
			return "no worker pools configured";
		}

		// 3. there should be at least one http consumer enabled (/admin/deploy)
		if (registry.getHttpConsumers().stream().noneMatch(HttpConsumer::isEnabled)) {
			return "no enabled http services found, not ready to accept traffic";
		}

		return null;
	}
}
