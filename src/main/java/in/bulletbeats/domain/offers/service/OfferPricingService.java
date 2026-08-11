package in.bulletbeats.domain.offers.service;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillItem;
import in.bulletbeats.domain.offers.entity.Offer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/** Pure discount-amount calculation for an offer against a bill — no persistence. */
@Service
public class OfferPricingService {

    public BigDecimal computeDiscount(Offer offer, Bill bill) {
        BigDecimal subtotal = bill.getSubtotal();
        return switch (offer.getMechanism()) {
            case BILL_PERCENTAGE -> percentageOf(subtotal, offer.getPercentageValue());
            case BILL_FIXED -> offer.getFixedValue().min(subtotal);
            case ITEM_PERCENTAGE -> matchingItems(offer, bill).stream()
                    .map(item -> percentageOf(item.getLineTotal(), offer.getPercentageValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case ITEM_FIXED -> matchingItems(offer, bill).stream()
                    .map(item -> offer.getFixedValue().min(item.getLineTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case BUY_X_GET_Y_FREE -> buyXGetYDiscount(offer, bill);
        };
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return base.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private List<BillItem> matchingItems(Offer offer, Bill bill) {
        return bill.getItems().stream()
                .filter(item -> offer.getItems().stream().anyMatch(scope -> scope.matches(item.getMenuItem())))
                .toList();
    }

    private BigDecimal buyXGetYDiscount(Offer offer, Bill bill) {
        List<BillItem> matching = matchingItems(offer, bill);
        if (matching.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int totalQty = matching.stream().mapToInt(BillItem::getQuantity).sum();
        int groupSize = offer.getBuyQuantity() + offer.getGetQuantity();
        int freeUnits = (totalQty / groupSize) * offer.getGetQuantity();
        if (freeUnits <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal cheapestUnitPrice = matching.stream()
                .map(BillItem::getUnitPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal matchingSubtotal = matching.stream()
                .map(BillItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cheapestUnitPrice.multiply(BigDecimal.valueOf(freeUnits)).min(matchingSubtotal);
    }
}
