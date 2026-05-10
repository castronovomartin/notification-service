package com.cobre.notification.adapter.out.webhook;

import com.cobre.notification.domain.model.DeliveryResult;
import java.util.function.Predicate;

public class NonSuccessResultPredicate implements Predicate<DeliveryResult> {

    @Override
    public boolean test(DeliveryResult result) {
        return !result.success();
    }
}
