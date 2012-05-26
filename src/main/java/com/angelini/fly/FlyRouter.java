package com.angelini.fly;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FlyRouter extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private RoutedServlet routedServlet;
	protected Map<Integer, RouteTreeNode> routeTrees;
	
	protected static RouteMatch match(RouteTreeNode tree, String route) {
		RouteMatch match = new RouteMatch();
		String[] split = route.split("/");
		
		if (split.length == 0) {
			match.setMethod(tree.getMethod());
			return match;
		}
		
		for (int i = 1; i < split.length; i++) {
			String segment = split[i];
			boolean leaf = (i == split.length - 1) ? true : false;
			
			RouteTreeNode node = tree.getMatchNextLevel(segment, leaf);
			
			if (node == null) {
				return match;
			}
			
			if (!node.isSpecific()) {
				match.setParam(node.getRoute().substring(1), segment);
			}
			
			if (leaf) {
				match.setMethod(node.getMethod());
			} else {
				tree = node;
			}
		}
		
		return match;
	}
	
	public FlyRouter(FlyDB db, Class<?> routed) throws RouterException {
		try {
			routedServlet = (RoutedServlet) routed.newInstance();
			routedServlet.init(db);
		} catch (Exception e) {
			throw new RouterException(e.getMessage());
		}
		
		routeTrees = new HashMap<Integer, RouteTreeNode>();
		routeTrees.put(Router.GET, null);
		routeTrees.put(Router.POST, null);
		routeTrees.put(Router.DELETE, null);
		routeTrees.put(Router.PUT, null);
		
		Method[] methods = routed.getMethods();
		
		for (Method method : methods) {
			if (method.isAnnotationPresent(Router.class)) {
				Router router = method.getAnnotation(Router.class);
				String route = router.route();
				int verb = router.verb();
				
				RouteTreeNode current = null;
				String[] split = route.split("/");
				
				if (split.length == 0) {
					split = new String[] {""};
				}
				
				for (int i = 0; i < split.length; i++) {
					String segment = split[i];
					RouteTreeNode node = null; 
					
					if (i == split.length - 1) {
						node = new RouteTreeNode(segment, method);
					} else {
						node = new RouteTreeNode(segment, null);
					}
					
					if (routeTrees.get(verb) == null) {
						routeTrees.put(verb, node);
					} else if (i == 0) {
						if (routeTrees.get(verb).getMethod() == null && node.getMethod() != null) {
							routeTrees.get(verb).setMethod(node.getMethod());
						}
						
						current = routeTrees.get(verb);
						continue;
					} else {
						current.addNode(node);
					}
					
					current = node;
				}
			}
		}
	}
	
	private void route(RouteTreeNode tree, HttpServletRequest req, HttpServletResponse resp) throws HttpException {
		HttpRequest httpRequest = new HttpRequest(req);
		HttpResponse httpResponse = new HttpResponse(resp);
		
		String path = (req.getPathInfo() == null) ? "/" : req.getPathInfo();
		RouteMatch match = FlyRouter.match(tree, path);
		
		httpRequest.setParams(match.getParams());
		
		if (match.getMethod() == null) {
			resp.setStatus(404);
			return;
		}
		
		try {
			match.getMethod().invoke(routedServlet, httpRequest, httpResponse);
		} catch (Exception e) {
			throw new HttpException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws HttpException, IOException {
		route(routeTrees.get(Router.GET), req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws HttpException, IOException {
		route(routeTrees.get(Router.POST), req, resp);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws HttpException, IOException {
		route(routeTrees.get(Router.DELETE), req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws HttpException, IOException {
		route(routeTrees.get(Router.PUT), req, resp);
	}

}
