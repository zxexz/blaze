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
 package com.nortal.fhir.rest.filter.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.message.Message;

public abstract class AbstractWriter<T> implements MessageBodyWriter<T> {

  protected abstract void writeTo(T t, String contentType, OutputStream entityStream) throws IOException;

  @Override
  public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;// funny javadoc
  }

  @Override
  public void writeTo(T t,
                      Class<?> type,
                      Type genericType,
                      Annotation[] annotations,
                      MediaType mediaType,
                      MultivaluedMap<String, Object> headers,
                      OutputStream entityStream)
      throws IOException, WebApplicationException {
    String ct = Message.CONTENT_TYPE;
    String contentType = headers.containsKey(ct) ? headers.getFirst(ct).toString() : null;
    writeTo(t, contentType, entityStream);
  }

}
