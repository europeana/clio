package eu.europeana.clio.reporting.rest;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class ClioReportingRestInitializer extends
        AbstractAnnotationConfigDispatcherServletInitializer {

  @Override
  protected Class<?>[] getRootConfigClasses() {
    return null;
  }

  @Override
  protected Class<?>[] getServletConfigClasses() {
    return new Class[]{ClioReportingRestApplication.class};
  }

  @Override
  protected String[] getServletMappings() {
    return new String[]{"/"};
  }
}
