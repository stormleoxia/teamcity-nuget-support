/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.tests.feed;

import com.sun.jersey.api.core.ExtendedUriInfo;
import jetbrains.buildServer.nuget.server.feed.FeedDescriptor;
import jetbrains.buildServer.nuget.server.feed.PackageEntry;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.odata4j.core.OEntity;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 18.07.11 1:03
 */
public class FeedTest extends XmlTestBase {
  private Mockery m;
  private ExtendedUriInfo info;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    info = m.mock(ExtendedUriInfo.class);

    m.checking(new Expectations(){{
      allowing(info).getBaseUri(); will(returnValue(new URI("http://feed.jonnyzzz.name/")));
      allowing(info).getPath(); will(returnValue("path/Packages"));
    }});
  }

  @Test
  public void testEmptyFeed() throws IOException, JDOMException {
    doTest(new FeedDescriptor().getEntities(Collections.<OEntity>emptyList(), empty()), "empty-feed.xml");
  }

  @Test
  public void testEmptyFeed_OneEntry() throws IOException, JDOMException {
    doTest(new FeedDescriptor().getEntities(Arrays.<OEntity>asList(new PackageEntry()), empty()), "one-entry.xml");
  }

  private QueryInfo empty() {
    return new QueryInfo(
            OptionsQueryParser.parseInlineCount(null),
            OptionsQueryParser.parseTop("90"),
            OptionsQueryParser.parseSkip("0"),
            OptionsQueryParser.parseFilter(null),
            OptionsQueryParser.parseOrderBy(null),
            OptionsQueryParser.parseSkipToken(null),
            Collections.<String, String>emptyMap(),
            OptionsQueryParser.parseExpand(null),
            OptionsQueryParser.parseSelect(null));
  }

  private void doTest(@NotNull final EntitiesResponse responce, @NotNull final String gold) throws IOException, JDOMException {

    FormatWriter<EntitiesResponse> atom = FormatWriterFactory.getFormatWriter(
            EntitiesResponse.class,
            Arrays.asList(MediaType.APPLICATION_ATOM_XML_TYPE),
            "atom",
            null
    );

    StringWriter sw = new StringWriter();
    atom.write(info, sw, responce);
    final String actual = sw.toString();
    assertXml(gold, actual);
  }


}