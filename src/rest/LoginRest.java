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
import model.Password;
import model.User;
import service.LoginService;

/**
 * @author Maciej Szaba³a
 */
@Path("/login")
public class LoginRest {

	private static LoginService loginService = LoginService.getInstance();
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(String json) {
		System.out.println("Login: " + json);
		String token = null;
		try {
			Jsonb jsonb = JsonbBuilder.create();
			Credentials user = jsonb.fromJson(json, Credentials.class);
			
			// 1. check if user exists in db
			// 2a. if exists, check if password is correct
			// 3a. if password is correct, return a token to the user
			// 
			// 2b. if user does not exist, create new account in a db
			// 3b. generate a salt
			// 4b. generate a hash
			// 5b. store user's id, salt and hash in a db
			// 6b. return a token to the user
			
			if( loginService.userExistsInDb(user.getUsername()) ) {
				System.out.println("User " + user.getUsername() + " exists in db.");
				if(!loginService.auth(user)) {
					throw new Exception("Password invalid");
				}
			} else {
				System.out.println("Creating new user account.");
				loginService.createNewAccount(user);
			}
			
			token = loginService.createJwt(user.getUsername());
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		ResponseBuilder rb = Response.ok();
		rb = loginService.defaultHeaders(rb);
		return rb.entity(token).build();
	}

}
