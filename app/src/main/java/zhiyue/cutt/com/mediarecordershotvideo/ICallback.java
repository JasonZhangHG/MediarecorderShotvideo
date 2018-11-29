package zhiyue.cutt.com.mediarecordershotvideo;

/**
 * Common callback for asynchronous operation.
 *
 * @param <RESULT>
 */
public interface ICallback<RESULT> {

    void onResult(RESULT result);

    void onError(Throwable error);

}
