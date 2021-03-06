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
 package com.kodality.blaze.store;

import com.kodality.blaze.util.sql.SimpleDataSource;
import com.kodality.blaze.whiplash.api.WhiplashRunner;
import liquibase.exception.LiquibaseException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.sql.DataSource;

import java.sql.SQLException;

@Component(immediate = true, service = WhiplashRunner.class)
public class StoreWhiplashRunner extends WhiplashRunner {
  @Reference
  private SimpleDataSource dataSource;

  public StoreWhiplashRunner() {
    super("pg-store/changeset.xml");
  }

  @Activate
  public void init() throws SQLException, LiquibaseException {
    run();
  }

  @Override
  protected BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(StoreWhiplashRunner.class).getBundleContext();
  }

  @Override
  protected DataSource getDataSource() {
    return dataSource;
  }

}
