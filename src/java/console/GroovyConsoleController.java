/*
 * Copyright 2007 Bruce Fancher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package console;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.iterative.groovy.service.GroovyConsoleService;


/**
 * 
 * @author Bruce Fancher
 * 
 */
public class GroovyConsoleController extends AbstractController {

    private GroovyConsoleService groovyConsoleService;
    
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        groovyConsoleService.launch();
        return null;
    }

    public void setGroovyConsoleService(GroovyConsoleService groovyConsoleService) {
        this.groovyConsoleService = groovyConsoleService;
    }
}
