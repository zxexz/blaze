/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.kodality.blaze.store.dao;

import com.kodality.blaze.core.model.ResourceVersion;
import com.kodality.blaze.core.model.VersionId;
import com.kodality.blaze.core.util.JsonUtil;
import com.kodality.blaze.fhir.structure.api.ResourceContent;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class ResourceRowMapper implements RowMapper<ResourceVersion> {

  @Override
  public ResourceVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
    ResourceVersion resource = new ResourceVersion();
    resource.setId(mapVersion(rs));
    resource.setContent(mapContent(rs));
    resource.setDeleted(rs.getString("sys_status").equals("C"));
    resource.setAuthor(JsonUtil.fromJson(rs.getString("author")));
    resource.setModified(new Date(rs.getTimestamp("last_updated").getTime()));
    return resource;
  }

  private ResourceContent mapContent(ResultSet rs) throws SQLException {
    return new ResourceContent(rs.getString("content"), "json");
  }

  private VersionId mapVersion(ResultSet rs) throws SQLException {
    return new VersionId(rs.getString("type"), rs.getString("id"), rs.getInt("last_version"));
  }

}
