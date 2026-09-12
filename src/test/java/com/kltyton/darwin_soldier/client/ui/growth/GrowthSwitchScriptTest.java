package com.kltyton.darwin_soldier.client.ui.growth;

import com.sighs.apricityui.forge.script.rhino.AuiRhinoContextBridge;
import dev.latvian.mods.rhino.Context;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GrowthSwitchScriptTest {
    @Test
    void abilitySwitchCallbacksSendRequestedStateOnRhino() throws Exception {
        Context context = AuiRhinoContextBridge.enter();
        var scope = context.initStandardObjects();
        context.evaluateString(scope, """
                var controls = [], events = [];
                var McUIVue = {McSwitch:'switch'};
                var DarwinUi = {
                    h:function(type, props){if(type==='switch')controls.push(props);return {};},
                    action:function(name, payload){events.push(name+':'+payload.enabled);},
                    button:function(){return {};}, panel:function(){return {};},
                    mount:function(render){render({
                        enabled:true, metrics:[], allocation:[], abilities:[], intel:[],
                        detail:{title:'Ability',lines:[],actions:[
                            {action:'toggle-ability',ability:'DAMAGE_ADAPTATION',label:'Enable',enabled:true},
                            {action:'toggle-hunting-impact',label:'Internal Impact',enabled:true},
                            {action:'toggle-ability',label:'Disabled',enabled:true,disabled:true},
                            {action:'open-adaptations',label:'Adaptations'}
                        ]}
                    });}
                };
                """, "switch-test-state", 1, null);
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "assets/apricityui/apricity/darwin_soldier/scripts/growth.js")) {
            assertNotNull(stream);
            context.evaluateString(scope, new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                    "growth.js", 1, null);
        }
        Object result = context.evaluateString(scope, """
                if(controls.length!==3)throw new Error('Missing ability switches');
                controls.forEach(function(control){
                    if(typeof control['onUpdate:modelValue']!=='function')
                        throw new Error('Switch callback is not bound');
                    control['onUpdate:modelValue'](false);
                    control['onUpdate:modelValue'](true);
                });
                events.join(',');
                """, "switch-requested-state", 1, null);
        assertEquals("toggle-ability:false,toggle-ability:true,toggle-hunting-impact:false,toggle-hunting-impact:true",
                result);
    }
}
