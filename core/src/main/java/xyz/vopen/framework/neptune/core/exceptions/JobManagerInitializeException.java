package xyz.vopen.framework.neptune.core.exceptions;

import xyz.vopen.framework.neptune.common.exceptions.NeptuneRuntimeException;

import javax.annotation.Nonnull;

/**
 * {@link JobManagerInitializeException}
 *
 * @author <a href="mailto:siran0611@gmail.com">Elias.Yao</a>
 * @version ${project.version} - 2020/10/11
 */
public class JobManagerInitializeException extends NeptuneRuntimeException {
  private static final long serialVersionUID = -1811246140334789689L;

  public JobManagerInitializeException() {
    super();
  }

  public JobManagerInitializeException(@Nonnull String message) {
    super(message);
  }

  public JobManagerInitializeException(@Nonnull Throwable cause) {
    super(cause);
  }

  public JobManagerInitializeException(String message, Throwable cause) {
    super(message, cause);
  }
}
