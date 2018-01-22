package com.nortal.blaze.search.dao;

import com.nortal.blaze.core.exception.ServerException;
import com.nortal.blaze.search.model.Blindex;
import com.nortal.blaze.util.sql.SqlBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(immediate = true, service = BlindexDao.class)
public class BlindexDao {
  private static final Map<String, String> parasols = new HashMap<String, String>();
  @Reference
  private JdbcTemplate jdbcTemplate;

  @Activate
  public void init() throws Exception {
    load(Blindex.PARASOL).forEach(p -> parasols.put(p.getKey(), p.getName()));
  }

  public static String getParasol(String resourceType, String path) {
    String key = resourceType + "." + path;
    if (!parasols.containsKey(key)) {
      throw new ServerException(key + " not indexed");
    }
    return parasols.get(key);
  }

  public List<Blindex> load() {
    return load(null);
  }

  public List<Blindex> load(String type) {
    SqlBuilder sb = new SqlBuilder();
    sb.append("SELECT resource_type, path, index_name FROM blindex");
    sb.appendIfNotNull("WHERE index_type = ?", type);
    return jdbcTemplate.query(sb.getSql(), sb.getParams(), new ParasolRowMapper());
  }

  public void createIndex(String resourceType, String path) {
    jdbcTemplate.queryForObject("SELECT create_blindex(?,?)", String.class, resourceType, path);
  }

  public void dropIndex(String resourceType, String path) {
    jdbcTemplate.queryForObject("SELECT drop_blindex(?,?)", String.class, resourceType, path);
  }

  private static class ParasolRowMapper implements RowMapper<Blindex> {

    @Override
    public Blindex mapRow(ResultSet rs, int rowNum) throws SQLException {
      Blindex p = new Blindex();
      p.setResourceType(rs.getString("resource_type"));
      p.setPath(rs.getString("path"));
      p.setName(rs.getString("index_name"));
      return p;
    }

  }

}
