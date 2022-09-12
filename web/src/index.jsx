import { Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { RecoilRoot } from 'recoil';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Container from '@mui/material/Container';
import StatusBar from './statusbar.jsx';
import NavBar from './nav-bar';
import Loader from './loader';
import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import './index.css';

const Portfolio = lazy(() => import('./portfolio.jsx'));
const Transactions = lazy(() => import('./transactions.jsx'));

const darkTheme = createTheme({
  palette: { mode: "dark" },
});

const App = () => (
  <ThemeProvider theme={darkTheme}>
    <CssBaseline />
      <NavBar />
      <Suspense fallback={<Loader />}>
        <Container>
          <Routes>
            <Route path="/" element={<Portfolio/>} />
            <Route path="tx" element={<Transactions/> } />
          </Routes>
        </Container>
      </Suspense>
      <StatusBar />
  </ThemeProvider>
);

createRoot(document.querySelector("#root")).render(
  <RecoilRoot>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </RecoilRoot>
);
