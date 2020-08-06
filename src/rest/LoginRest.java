package rest;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import model.Credentials;
import service.LoginService;

@Path("/login")
public class LoginRest {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(String json) {
		System.out.println("Login: " + json);
		String token = null;
		try {
			Jsonb jsonb = JsonbBuilder.create();
			Credentials user = jsonb.fromJson(json, Credentials.class);
			token = LoginService.createJwt(user.getUsername());
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		ResponseBuilder rb = Response.ok();
		rb = LoginService.defaultHeaders(rb);
		return rb.entity(token).build();
	}

}
