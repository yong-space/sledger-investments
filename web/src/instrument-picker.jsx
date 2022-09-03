import { useState } from 'react';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import Api from './api';

const debounce = (func, timeout = 300) => {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => { func.apply(undefined, args); }, timeout);
    };
};

const InstrumentPicker = ({ instrument, setInstrument }) => {
    const [ options, setOptions ] = useState([]);
    const { searchInstrument } = Api();

    const search = debounce((event, inputValue) => {
        if (inputValue.trim().length === 0) {
            setOptions([]);
            return;
        }
        searchInstrument(inputValue, (response) => setOptions(response['Data']));
    }, 300);

    return (
        <Autocomplete
            filterOptions={(x) => x}
            options={options}
            value={instrument}
            onInputChange={search}
            onChange={(event, newValue) => setInstrument(newValue)}
            renderInput={(params) => <TextField fullWidth required {...params} name="instrument" size="small" label="Instrument" />}
            getOptionLabel={(option) => `${option.Symbol} ${option['Description']}`}
        />
    );
};
export default InstrumentPicker;
