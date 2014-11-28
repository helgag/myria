package edu.washington.escience.myria.api;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.glassfish.grizzly.compression.zip.GZipEncoder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wordnik.swagger.jaxrs.config.BeanConfig;

import edu.washington.escience.myria.daemon.MasterDaemon;
import edu.washington.escience.myria.parallel.Server;

/**
 * This object simply configures which resources can be requested via the REST server.
 * 
 */
public final class MasterApplication extends ResourceConfig {

  /**
   * Instantiate the main application running on the Myria master.
   * 
   * @param server the Myria server running on this master.
   * @param daemon the Myria daemon running on this master.
   */
  public MasterApplication(final Server server, final MasterDaemon daemon) {
    /*
     * Tell Jersey to look for resources inside the entire project, and also for Swagger.
     */
    packages(new String[] { "edu.washington.escience.myria", "com.wordnik.swagger.jersey.listing" });

    /*
     * Disable WADL - throws error messages when using Swagger, and not needed.
     */
    property(ServerProperties.WADL_FEATURE_DISABLE, true);

    /* Enable Jackson's JSON Serialization/Deserialization. */
    register(JacksonJsonProvider.class);

    /* Enable Multipart. */
    register(MultiPartFeature.class);

    /* Register the binder. */
    registerInstances(new SingletonBinder(server, daemon));

    /* Enable GZIP compression/decompression */
    register(EncodingFilter.class);
    register(GZipEncoder.class);

    /* Swagger configuration -- must come BEFORE Swagger classes are added. */
    BeanConfig myriaBeanConfig = new BeanConfig();
    /*
     * TODO(dhalperi): make this more dynamic based on either Catalog or runtime option.
     */
    myriaBeanConfig.setBasePath("http://rest.myria.cs.washington.edu:1776");
    myriaBeanConfig.setVersion("0.1.0");
    myriaBeanConfig.setResourcePackage("edu.washington.escience.myria.api");
    myriaBeanConfig.setScan(true);

    /*
     * Add a response filter (i.e., runs on all responses) that sets headers for cross-origin objects.
     */
    register(new CrossOriginResponseFilter());
  }

  /** Binder to bind server and daemon. */
  public static class SingletonBinder extends AbstractBinder {

    /** the server singleton. */
    private final Server server;
    /** the master daemon singleton. */
    private final MasterDaemon daemon;

    /**
     * Constructor.
     * 
     * @param server the server singleton.
     * @param daemon the master daemon singleton.
     * */
    SingletonBinder(final Server server, final MasterDaemon daemon) {
      this.server = server;
      this.daemon = daemon;
    }

    @Override
    protected void configure() {
      bind(server).to(Server.class);
      bind(daemon).to(MasterDaemon.class);
    }
  }

  /**
   * This is a container response filter. It will run on all responses leaving the server and add the CORS filters
   * saying that these API calls should be allowed from any website. This is a mechanism increasingly supported by
   * modern browsers instead of, e.g., JSONP.
   * 
   * For more information, visit http://www.w3.org/TR/cors/ and http://enable-cors.org/
   * 
   * TODO revisit the security of this model
   * 
   * 
   */
  private class CrossOriginResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(final ContainerRequestContext request, final ContainerResponseContext response) {
      response.getHeaders().add("Access-Control-Allow-Origin", "*");
      response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
      response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }
  }
}
