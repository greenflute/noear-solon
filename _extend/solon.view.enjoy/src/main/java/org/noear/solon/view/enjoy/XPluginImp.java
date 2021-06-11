package org.noear.solon.view.enjoy;

import com.jfinal.template.Directive;
import org.noear.solon.SolonApp;
import org.noear.solon.Utils;
import org.noear.solon.core.Aop;
import org.noear.solon.core.Bridge;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.handle.RenderManager;
import org.noear.solon.view.enjoy.tags.HasPermissionTag;
import org.noear.solon.view.enjoy.tags.HasRoleTag;

@SuppressWarnings("unchecked")
public class XPluginImp implements Plugin {
    public static boolean output_meta = false;

    @Override
    public void start(SolonApp app) {
        output_meta = app.cfg().getInt("solon.output.meta", 0) > 0;

        EnjoyRender render =  EnjoyRender.global();

        Aop.beanOnloaded(() -> {
            Aop.beanForeach((k, v) -> {
                if (k.startsWith("view:")) { //java view widget
                    if(Directive.class.isAssignableFrom(v.clz())){
                        render.addDirective(k.split(":")[1], (Class<? extends Directive>)v.clz());
                    }
                    return;
                }

                if(k.startsWith("share:")){ //java share object
                    render.setSharedVariable(k.split(":")[1], v.raw());
                    return;
                }
            });
        });

        RenderManager.register(render);
        RenderManager.mapping(".shtm",render);

        if (Utils.loadClass("org.noear.solon.extend.auth.AuthUtil") != null) {
            app.beanMake(HasPermissionTag.class);
            app.beanMake(HasRoleTag.class);
        }
    }
}
