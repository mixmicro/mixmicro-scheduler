package xyz.vopen.framework.repository.mysql;

import com.google.common.collect.Lists;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import xyz.vopen.framework.neptune.common.configuration.Configuration;
import xyz.vopen.framework.neptune.common.model.InstanceInfo;
import xyz.vopen.framework.neptune.common.model.JobInfo;
import xyz.vopen.framework.neptune.common.model.ServerInfo;
import xyz.vopen.framework.neptune.repository.api.BaseRepository;
import xyz.vopen.framework.neptune.repository.api.InstanceInfoRepository;
import xyz.vopen.framework.neptune.repository.api.JobRepository;
import xyz.vopen.framework.neptune.repository.api.ServerRepository;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static xyz.vopen.framework.neptune.common.configuration.PersistenceOptions.*;

/**
 * {@link MysqlRepository}
 *
 * @author <a href="mailto:siran0611@gmail.com">Elias.Yao</a>
 * @version ${project.version} - 2020/10/20
 */
public class MysqlRepository
    implements BaseRepository, ServerRepository, JobRepository, InstanceInfoRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MysqlRepository.class);

  private final Configuration configuration;
  private final @Nonnull MySQLPool client;

  private MysqlRepository(final Configuration configuration) {
    this.configuration = configuration;

    MySQLConnectOptions connectOptions =
        new MySQLConnectOptions()
            .setPort(configuration.getInteger(MYSQL_PORT))
            .setHost(configuration.getString(MYSQL_ADDRESS))
            .setDatabase(configuration.getString(MYSQL_DATABASE))
            .setUser(configuration.getString(MYSQL_USER))
            .setPassword(configuration.getString(MYSQL_PASSWORD));
    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    this.client = MySQLPool.pool(connectOptions, poolOptions);
  }

  public static MysqlRepository createFromConfiguration(
      final @Nonnull Configuration configuration) {
    return new MysqlRepository(configuration);
  }

  // =====================  Server Info   =====================
  /**
   * Returns the specifies server message through specifies the server name, allows null.
   *
   * @param serverName of the server.
   * @return The specifies server message.
   */
  @Override
  public Optional<ServerInfo> queryServerByName(@Nonnull String serverName) {
    AtomicReference<ServerInfo> serverInfo = null;
    client
        .preparedQuery("SELECT * FROM server_info WHERE server_name = ? ")
        .execute(
            Tuple.of(serverName),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, ServerInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    serverInfo.set((ServerInfo) ret.get(0));
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              } else {
                LOG.error("[MysqlRepository] failure: {}", ar.cause().getMessage());
              }
            });

    return Optional.ofNullable(serverInfo.get());
  }

  /**
   * Returns all servers message,allows null.
   *
   * @return The all servers message.
   */
  @Override
  public Optional<List<ServerInfo>> queryServers() {
    AtomicReference<List<ServerInfo>> serverInfo = null;
    client
        .query("SELECT * FROM server_info")
        .execute(
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, ServerInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    serverInfo.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(serverInfo.get());
  }

  /**
   * Save the server message.
   *
   * @param serverInfo {@link ServerInfo} instance.
   * @return
   */
  @Override
  public void saveServerInfo(@Nonnull ServerInfo serverInfo) {
    client
        .preparedQuery("INSERT INTO server_info (id,address,server_name) VALUES (?,?,?)")
        .execute(
            Tuple.of(serverInfo.getId(), serverInfo.getAddress(), serverInfo.getServiceName()),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                long lastInsertId = result.property(MySQLClient.LAST_INSERTED_ID);
                LOG.info("[MysqlRepository] saveServerInfo Last inserted id is: {}", lastInsertId);
              } else {
                LOG.error("[MysqlRepository] failure: {}", ar.cause().getMessage());
              }
            });
  }

  // =====================   Job Info  =====================
  /**
   * Returns the specify job through specify the job id.
   *
   * @param jobId of job.
   * @return Job collection.
   */
  @Override
  public Optional<JobInfo> findJobById(long jobId) {
    AtomicReference<JobInfo> jobInfo = null;
    client
        .preparedQuery("SELECT * FROM job_info WHERE id = ? ")
        .execute(
            Tuple.of(jobId),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, JobInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    jobInfo.set((JobInfo) ret.get(0));
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(jobInfo.get());
  }

  /**
   * Returns the specify job collection under server.
   *
   * @param appId Represent the server id.
   * @return Job collection.
   */
  @Override
  public Optional<List<JobInfo>> findJobByAppId(long appId) {
    AtomicReference<List<JobInfo>> jobInfo = null;
    client
        .preparedQuery("SELECT * FROM job_info WHERE app_id = ?")
        .execute(
            Tuple.of(appId),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, JobInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    jobInfo.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(jobInfo.get());
  }

  /**
   * Returns the specify job collection through appId and job name.
   *
   * @param appId server id.
   * @param name of job.
   * @return Job collection.
   */
  @Override
  public Optional<List<JobInfo>> findJobByAppIdAndName(long appId, String name) {
    AtomicReference<List<JobInfo>> jobInfo = null;
    client
        .preparedQuery("SELECT * FROM job_info WHERE app_id = ? and name = ?")
        .execute(
            Tuple.of(appId, name),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, JobInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    jobInfo.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(jobInfo.get());
  }

  @Override
  public Optional<List<JobInfo>> findJobByAppIdAndStatus(long appId, int status) {
    AtomicReference<List<JobInfo>> jobInfo = null;
    client
        .preparedQuery("SELECT * FROM job_info WHERE app_id = ? and status = ?")
        .execute(
            Tuple.of(appId, status),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, JobInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    jobInfo.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(jobInfo.get());
  }

  /**
   * Save the job message.
   *
   * @param jobInfo {@link JobInfo} instance.
   * @return
   */
  @Override
  public void saveJobInfo(JobInfo jobInfo) {
    client
        .preparedQuery(
            "INSERT INTO job_info (id,job_name,job_description,app_id,job_params,time_expression_type,"
                + "time_expression,execute_type,processor_type,processor_info,max_instance_num,concurrency,"
                + "instance_time_limit,instance_retry_num,task_retry_num,status,next_trigger_time,min_cpu_cores,min_memory_space,"
                + "min_disk_space,designated_workers,max_worker_count,notify_user_ids) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
        .execute(
            Tuple.of(
                jobInfo.getId(),
                jobInfo.getJobName(),
                jobInfo.getJobDescription(),
                jobInfo.getAppId(),
                jobInfo.getJobParams(),
                jobInfo.getTimeExpressionType(),
                jobInfo.getTimeExpression(),
                jobInfo.getExecuteType(),
                jobInfo.getProcessorType(),
                jobInfo.getProcessorInfo(),
                jobInfo.getMaxInstanceNum(),
                jobInfo.getConcurrency(),
                jobInfo.getInstanceTimeLimit(),
                jobInfo.getInstanceRetryNum(),
                jobInfo.getTaskRetryNum(),
                jobInfo.getStatus(),
                jobInfo.getNextTriggerTime(),
                jobInfo.getMinCpuCores(),
                jobInfo.getMinMemorySpace(),
                jobInfo.getMinDiskSpace(),
                jobInfo.getDesignatedWorkers(),
                jobInfo.getMaxWorkerCount(),
                jobInfo.getNotifyUserIds()),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                long lastInsertId = result.property(MySQLClient.LAST_INSERTED_ID);
                LOG.info("[MysqlRepository] saveJobInfo Last inserted id is: {}", lastInsertId);
              } else {
                LOG.error("[MysqlRepository] failure: {}", ar.cause().getMessage());
              }
            });
  }

  /**
   * Update the job message.
   *
   * @param jobInfo {@link JobInfo instance.}
   * @return
   */
  @Override
  public void updateJobInfo(JobInfo jobInfo) {
    client
        .preparedQuery(
            "UPDATE job_info SET job_name = ?,job_description = ?,app_id = ?, job_params = ?, time_expression_type = ?,"
                + "time_expression = ?,execute_type = ?,processor_type = ?,processor_info = ?,max_instance_num = ?,concurrency = ?,"
                + "instance_time_limit = ?,instance_retry_num = ?,task_retry_num = ?,status = ?,next_trigger_time = ?,min_cpu_cores = ?,"
                + "min_memory_space = ?,min_disk_space = ?,designated_workers = ?,max_worker_count = ?,notify_user_ids = ? WHERE  id = ?")
        .execute(
            Tuple.of(
                jobInfo.getJobName(),
                jobInfo.getJobDescription(),
                jobInfo.getAppId(),
                jobInfo.getJobParams(),
                jobInfo.getTimeExpressionType(),
                jobInfo.getTimeExpression(),
                jobInfo.getExecuteType(),
                jobInfo.getProcessorType(),
                jobInfo.getProcessorInfo(),
                jobInfo.getMaxInstanceNum(),
                jobInfo.getConcurrency(),
                jobInfo.getInstanceTimeLimit(),
                jobInfo.getInstanceRetryNum(),
                jobInfo.getTaskRetryNum(),
                jobInfo.getStatus(),
                jobInfo.getNextTriggerTime(),
                jobInfo.getMinCpuCores(),
                jobInfo.getMinMemorySpace(),
                jobInfo.getMinDiskSpace(),
                jobInfo.getDesignatedWorkers(),
                jobInfo.getMaxWorkerCount(),
                jobInfo.getNotifyUserIds(),
                jobInfo.getId()),
            ar -> {
              if (ar.succeeded()) {

              } else {
                LOG.error("[MysqlRepository] updateJobInfo failure: {}", ar.cause().getMessage());
              }
            });
  }

  /**
   * Delete the job by specified job id.
   *
   * @param jobId
   */
  @Override
  public void deleteJobInfos(Long jobId) {
    client
        .preparedQuery("DELETE FROM job_info WHERE id = ?")
        .execute(
            Tuple.of(jobId),
            ar -> {
              if (ar.succeeded()) {

              } else {
                LOG.error("[MysqlRepository] deleteJobInfos failure: {}", ar.cause().getMessage());
              }
            });
  }

  @Override
  public long countByJobIdAndStatus(long jobId, List<Integer> status) {
    AtomicLong count = new AtomicLong();
    client
        .preparedQuery("SELECT (*) FROM instance_info WHERE id = ? AND status in (?)")
        .execute(
            Tuple.of(jobId, status),
            ar -> {
              if (ar.succeeded()) {
                count.set(ar.result().iterator().next().getColumnIndex("count"));
              } else {
                LOG.error(
                    "[MysqlRepository] countByJobIdAndStatus failure: {}", ar.cause().getMessage());
              }
            });
    return count.get();
  }

  @Override
  public Optional<InstanceInfo> findByInstanceId(long instanceId) {
    AtomicReference<InstanceInfo> instanceInfo = null;
    client
        .preparedQuery("SELECT * FROM instance_info WHERE id = ? ")
        .execute(
            Tuple.of(instanceId),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, InstanceInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    instanceInfo.set((InstanceInfo) ret.get(0));
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(instanceInfo.get());
  }

  @Override
  public Optional<List<InstanceInfo>> findByJobIdAndStatus(long jobId, List<Integer> status) {
    AtomicReference<List<InstanceInfo>> instanceInfos = null;
    client
        .preparedQuery("SELECT * FROM instance_info WHERE job_id = ? and status in (?)")
        .execute(
            Tuple.of(jobId, status),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, InstanceInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    instanceInfos.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(instanceInfos.get());
  }

  @Override
  public Optional<List<InstanceInfo>> findInstancesByAppId(long appId) {
    AtomicReference<List<InstanceInfo>> instanceInfos = null;
    client
        .preparedQuery("SELECT * FROM instance_info WHERE app_id = ?")
        .execute(
            Tuple.of(appId),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, InstanceInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    instanceInfos.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(instanceInfos.get());
  }

  @Override
  public Optional<List<InstanceInfo>> findInstancesByAppIdAndStatus(long appId, int status) {
    AtomicReference<List<InstanceInfo>> instanceInfos = null;
    client
        .preparedQuery("SELECT * FROM instance_info WHERE app_id = ? AND status = ?")
        .execute(
            Tuple.of(appId, status),
            ar -> {
              if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                try {
                  List ret = convert(result, InstanceInfo.class);
                  if (!CollectionUtils.isEmpty(ret)) {
                    instanceInfos.set(ret);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    return Optional.ofNullable(instanceInfos.get());
  }

  @Override
  public void saveInstanceInfo(InstanceInfo instanceInfo) {
    client
        .preparedQuery(
            "INSERT INTO instance_info (id,app_id,job_id,job_params,trigger_time,completed_time,last_report_time,"
                + "execute_time,result,status,type,work_flow_id,task_address,retry_times,gmt_create,gmt_update) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")
        .execute(
            Tuple.of(
                instanceInfo.getId(),
                instanceInfo.getAppId(),
                instanceInfo.getJobId(),
                instanceInfo.getJobParams(),
                instanceInfo.getTriggerTime(),
                instanceInfo.getCompletedTime(),
                instanceInfo.getLastReportTime(),
                instanceInfo.getExecuteTime(),
                instanceInfo.getResult(),
                instanceInfo.getStatus(),
                instanceInfo.getType(),
                instanceInfo.getWorkFlowId(),
                instanceInfo.getTaskAddress(),
                instanceInfo.getRetryTimes()),
            ar -> {
              if (ar.succeeded()) {
                LOG.info(
                    "[MysqlRepository] saveInstanceInfo success insert id: {}",
                    ar.result().property(MySQLClient.LAST_INSERTED_ID));
              } else {
                LOG.error(
                    "[MysqlRepository] saveInstanceInfo failure: {}", ar.cause().getMessage());
              }
            });
  }

  @Override
  public void updateInstanceInfo(InstanceInfo instanceInfo) {
    client
        .preparedQuery(
            "UPDATE instance_info SET app_id = ?,job_id = ?, job_params = ?, trigger_time = ?,"
                + "completed_time = ?,last_report_time = ?,execute_time = ?,result = ?,status = ?,type = ?,"
                + "work_flow_id = ?,task_address = ?,retry_times = ? WHERE  id = ?")
        .execute(
            Tuple.of(
                instanceInfo.getAppId(),
                instanceInfo.getJobId(),
                instanceInfo.getJobParams(),
                instanceInfo.getTriggerTime(),
                instanceInfo.getCompletedTime(),
                instanceInfo.getLastReportTime(),
                instanceInfo.getExecuteTime(),
                instanceInfo.getResult(),
                instanceInfo.getStatus(),
                instanceInfo.getType(),
                instanceInfo.getWorkFlowId(),
                instanceInfo.getTaskAddress(),
                instanceInfo.getRetryTimes(),
                instanceInfo.getId()),
            ar -> {
              if (ar.succeeded()) {

              } else {
                LOG.error(
                    "[MysqlRepository] updateInstanceInfo failure: {}", ar.cause().getMessage());
              }
            });
  }

  @Override
  public void deleteInstance(Long instanceId) {
    client
        .preparedQuery("DELETE FROM instance_info WHERE id = ?")
        .execute(
            Tuple.of(instanceId),
            ar -> {
              if (ar.succeeded()) {

              } else {
                LOG.error("[MysqlRepository] deleteInstance failure: {}", ar.cause().getMessage());
              }
            });
  }

  public void close() {
    if (client != null) {
      client.close();
    }
  }

  /**
   * Convert to PO through {@link RowSet}.
   *
   * @param rs {@link RowSet} instance.
   * @param clazz converted object.
   * @param <T>
   * @return
   * @throws IllegalAccessException Thrown when field is private.
   * @throws InstantiationException
   */
  static <T> List convert(RowSet<Row> rs, Class clazz)
      throws IllegalAccessException, InstantiationException {
    int count = rs.rowCount();
    List ret = Lists.newArrayList();

    Field[] fields = clazz.getDeclaredFields();
    while (rs.iterator().hasNext()) {
      Object newInstance = clazz.newInstance();
      for (int i = 1; i <= count; i++) {
        try {
          Row row = rs.iterator().next();
          for (int j = 0; j < fields.length; j++) {
            Field field = fields[j];
            String name = field.getName();
            Object o = row.get(field.getType(), j);
            if (name.equalsIgnoreCase(row.getColumnName(j))) {
              field.set(newInstance, row.get(field.getType(), j));
              field.setAccessible(field.isAccessible());
            }
          }
        } catch (Exception e) {

        }
        ret.add(newInstance);
      }
    }
    return ret;
  }
}
