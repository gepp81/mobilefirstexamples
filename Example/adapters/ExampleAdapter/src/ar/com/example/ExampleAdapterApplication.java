/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package ar.com.example;

import java.util.logging.Logger;
import com.worklight.wink.extensions.MFPJAXRSApplication;

public class ExampleAdapterApplication extends MFPJAXRSApplication {

  static Logger logger = Logger.getLogger(ExampleAdapterApplication.class.getName());

  @Override
  protected void init() throws Exception {
    logger.info("Adapter initialized!");
    ExampleAdapterResource.init();
  }

  @Override
  protected void destroy() throws Exception {
    logger.info("Adapter destroyed!");
  }

  @Override
  protected String getPackageToScan() {
    return getClass().getPackage().getName();
  }
}
