/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data;

import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.CommandModule;
import static edu.umass.cs.gns.gnsApp.clientCommandProcessor.commandSupport.GnsProtocolDefs.*;
import edu.umass.cs.gns.gnsApp.clientCommandProcessor.commandSupport.UpdateOperation;

/**
 *
 * @author westy
 */
public class SubstituteUnsigned extends AbstractUpdate {

  /**
   *
   * @param module
   */
  public SubstituteUnsigned(CommandModule module) {
    super(module);
  }

  /**
   *
   * @return
   */
  @Override
  public UpdateOperation getUpdateOperation() {
    return UpdateOperation.SINGLE_FIELD_SUBSTITUTE;
  }

  @Override
  public String getCommandName() {
    return SUBSTITUTE;
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{GUID, FIELD, VALUE, OLDVALUE};
  }

  @Override
  public String getCommandDescription() {
    return "Replaces oldvalue with newvalue in the key value pair for the given GUID. See below for more on the signature."
            + " Field must be world writeable as this command does not specify the writer and is not signed.";
  }
}