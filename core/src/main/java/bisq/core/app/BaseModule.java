package bisq.core.app;

import bisq.core.util.Initializable;

import bisq.common.app.AppModule;
import bisq.common.config.Config;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class BaseModule extends AppModule {

    public BaseModule(Config config) {
        super(config);
    }

    @Override
    protected void configure() {
        addInitializableSupport();
    }

    protected void addInitializableSupport() {
        final InjectionListener<Initializable> injectionListener = Initializable::initialize;
        TypeListener disposableListener = new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                if (Initializable.class.isAssignableFrom(type.getRawType())) {
                    @SuppressWarnings("unchecked")
                    TypeEncounter<Initializable> disposableEncounter = (TypeEncounter<Initializable>) encounter;
                    disposableEncounter.register(injectionListener);
                }
            }
        };
        bindListener(new AbstractMatcher<TypeLiteral<?>>() {
            @Override
            public boolean matches(TypeLiteral<?> typeLiteral) {
                return Initializable.class.isAssignableFrom(typeLiteral.getRawType());
            }

        }, disposableListener);

    }
}
