package com.redhat.emergency.response.disaster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

    private static Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public Completable rxStart() {

        ConfigStoreOptions localStore = new ConfigStoreOptions().setType("file").setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("path", System.getProperty("vertx-config-path")));
        ConfigStoreOptions configMapStore = new ConfigStoreOptions()
                .setType("configmap")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("name", System.getProperty("application.configmap", "disaster-service"))
                        .put("key", System.getProperty("application.configmap.key", "application-config.yaml")));
        ConfigStoreOptions ssoConfigMapStore = new ConfigStoreOptions()
                .setType("configmap")
                .setFormat("yaml")
                .setConfig(new JsonObject()
                        .put("name", System.getProperty("sso.configmap", "sso-config")));
        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        if (System.getenv("KUBERNETES_NAMESPACE") != null) {
            //we're running in Kubernetes
            options.addStore(configMapStore);
            options.addStore(ssoConfigMapStore);
        } else {
            //default to local config
            options.addStore(localStore);
        }
        
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        return retriever.rxGetConfig()
                .flatMapCompletable(json -> {
                    JsonObject rest = json; //sso-config is created without an overarching key
                    return vertx.rxDeployVerticle(RestApiVerticle::new, new DeploymentOptions().setConfig(rest)).ignoreElement();
                });
    }
}
