package fu.mss301.order.controller;
import fu.mss301.order.entity.Invoice;
import fu.mss301.order.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<Invoice> getAll() { return invoiceService.findAll(); }

    @GetMapping("/{id}")
    public Invoice getById(@PathVariable Long id) { return invoiceService.findById(id); }

    @GetMapping("/order/{orderId}")
    public Invoice getByOrderId(@PathVariable Long orderId) {
        return invoiceService.findByOrderId(orderId);
    }

    @PostMapping
    public Invoice create(@RequestParam Long orderId,
                          @RequestParam(required = false) String paymentMethod) {
        return invoiceService.createInvoice(orderId, paymentMethod);
    }

    @PatchMapping("/{id}/status")
    public Invoice updateStatus(@PathVariable Long id, @RequestParam String status) {
        return invoiceService.updateStatus(id, status);
    }
}
