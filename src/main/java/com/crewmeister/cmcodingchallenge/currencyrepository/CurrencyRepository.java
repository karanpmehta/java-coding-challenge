package com.crewmeister.cmcodingchallenge.currencyrepository;


import com.crewmeister.cmcodingchallenge.currency.Currency;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CurrencyRepository extends JpaRepository<Currency,Long> {
}
