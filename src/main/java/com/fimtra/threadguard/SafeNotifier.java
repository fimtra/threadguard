/*
 * Copyright (c) 2016 Ramon Servadei 
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fimtra.threadguard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/**
 * TODO
 * 
 * @author Ramon Servadei
 */
public class SafeNotifier
{
    final Executor[] executors;
    
    public SafeNotifier(Executor... executors)
    {
        this.executors = executors;
    }
    
    void safeNotify(Object context, Runnable runnable)
    {
        this.executors[context.hashCode() % this.executors.length].execute(runnable);
    }

    @SuppressWarnings("unchecked")
    <T> T wrap(Class<T> targetCallback, final Object target)
    {
        final InvocationHandler handler = new InvocationHandler()
        {
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
            {
                // todo only works with void returns
                if (method.getReturnType().equals(Void.TYPE))
                {
                    safeNotify(method, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                method.invoke(target, args);
                            }
                            catch (Exception e)
                            {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });
                }
                return null;
            }
        };
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[] { targetCallback }, handler);
    }
}
