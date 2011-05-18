package org.basex.core.cmd;

import static org.basex.core.Text.*;
import org.basex.core.CommandBuilder;
import org.basex.core.Command;
import org.basex.core.User;
import org.basex.core.Commands.Cmd;
import org.basex.core.Commands.CmdAlter;

/**
 * Evaluates the 'alter user' command and alters the password of a user.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class AlterUser extends Command {
  /**
   * Default constructor.
   * @param name user name
   * @param pw password
   */
  public AlterUser(final String name, final String pw) {
    super(User.ADMIN, name, pw);
  }

  @Override
  protected boolean run() {
    final String user = args[0];
    final String pass = args[1];
    if(!validName(user, false)) return error(NAMEINVALID, user);
    return pass == null || pass.isEmpty() ? error(PASSNO, user) :
      context.users.alter(user, pass) ?
      info(USERALTER, user) : error(USERNO, user);
  }

  @Override
  public void build(final CommandBuilder cb) {
    cb.init(Cmd.ALTER + " " + CmdAlter.USER).args();
  }
}
